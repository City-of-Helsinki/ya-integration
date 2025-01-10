package fi.hel.integration.ya.maksuliikenne;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;

import fi.hel.integration.ya.JsonValidator;
import fi.hel.integration.ya.RedisProcessor;
import fi.hel.integration.ya.SendEmail;
import fi.hel.integration.ya.SftpProcessor;
import fi.hel.integration.ya.exceptions.JsonValidationException;
import fi.hel.integration.ya.maksuliikenne.processor.MaksuliikenneProcessor;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class InMaksuliikenneRouteBuilder extends RouteBuilder {

    @Inject
    MaksuliikenneProcessor mlProcessor;

    @Inject
    JsonValidator jsonValidator;

    @Inject
    RedisProcessor redisProcessor;

    @Inject
    SftpProcessor sftpProcessor;

    @Inject
    SendEmail sendEmail;

    private final String SCHEMA_FILE_PT_PT55_TOJT = "schema/kipa/json_schema_PT_PT55_TOJT.json";
    private final String SCHEMA_FILE_MYK_HKK = "schema/kipa/json_schema_MYK_HKK.json";
    private final String MAKSULIIKENNE_EMAIL_RECIPIENTS = "{{MAKSULIIKENNE_EMAIL_RECIPIENTS}}";
    private final String MAKSULIIKENNE_NOFILES_EMAIL_RECIPIENTS = "{{MAKSULIIKENNE_NOFILES_EMAIL_RECIPIENTS}}";
    private final String JSON_ERROR_EMAIL_RECIPIENTS = "{{JSON_ERROR_EMAIL_RECIPIENTS}}";


    private final String LOCK_KEY = "timer-route-lock";
    
    @Override
    public void configure() throws Exception {

         // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.

        onException(JsonValidationException.class)
            .handled(true)
            .process(exchange -> {
                JsonValidationException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, JsonValidationException.class);

                Sentry.withScope(scope -> {
                    String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                    String uniqueId = UUID.randomUUID().toString(); // Generate a unique ID for the error

                    scope.setLevel(cause.getSentryLevel());
                    scope.setTag("error.type", cause.getTag());
                    scope.setTag("context.fileName", fileName);
                    scope.setFingerprint(Arrays.asList(uniqueId)); 
                    Sentry.captureException(cause);
                });

                Sentry.flush(2000);

            })
            .log("JsonValidationException occurred: ${exception.message}")
        ;

        // This route is for local development and testing
        // The route is triggered by dropping json file/files into folder inbox/kipa/P24
        from("file:inbox/kipa/P24")
            //.log("body :: ${body}")
            .log("Validating json file :: ${header.CamelFileName}")
            .process(exchange -> exchange.setVariable("combinedJsons", new ArrayList<Map<String,Object>>()))
            //.log("Body before validating :: ${body}")
            .to("direct:validate-json-P24")
            .choice()
                .when(simple("${header.isJsonValid} == 'true'"))
                    .log("Json is valid continue processing ${header.CamelFileName}")
                    .to("direct:continue-processing-P24Data")
                .otherwise()
                    .log("Json is not valid, ${header.CamelFileName}")
                    .throwException(new JsonValidationException("Invalid json file", SentryLevel.ERROR, "jsonValidationError"))
                    .to("file:outbox/invalidJson")
        ;

        from("{{MAKSULIIKENNE_QUARTZ_TIMER}}")
            .routeId("kipa-P24")
            .autoStartup("{{MAKSULIIKENNE_IN_AUTOSTARTUP}}")
            .process(exchange -> {
                if (redisProcessor.acquireLock(LOCK_KEY, 300)) { 
                    exchange.getIn().setHeader("lockAcquired", true);
                    System.out.println("Lock acquired, processing starts");

                } else {
                    exchange.getIn().setHeader("lockAcquired", false);
                    System.out.println("Lock not acquired, skipping processing");
                }
            })
            .filter(header("lockAcquired").isEqualTo(true))
                .log("Start route to fetch files from kipa P24")
                .setHeader("hostname").simple("{{KIPA_SFTP_HOST}}")
                .setHeader("username").simple("{{KIPA_SFTP_USER_P24}}")
                .setHeader("password").simple("{{KIPA_SFTP_PASSWORD_P24}}")
                .setHeader("directoryPath").simple("{{KIPA_DIRECTORY_PATH_P24}}")
                .setHeader("kipa_container", simple("P24"))
                //.setHeader("filePrefix", constant("YA_p24_091_2024121617"))
                //.setHeader("filePrefix2", constant("YA_p24_091_20241216163824_091_TOJT.json.json"))
                .log("Fetching file names from Kipa")
                .bean("sftpProcessor", "getAllSFTPFileNames")
                .choice()
                    .when(simple("${body} == null || ${body.size()} == 0"))
                        .log("No files found in SFTP.")
                        .setHeader("emailRecipients", constant(MAKSULIIKENNE_NOFILES_EMAIL_RECIPIENTS))
                        .process(ex -> {
                            String message = "Maksuliikenteen hyväksyttyjä maksuja ei ollut tälle päivälle <br><br><br>"
                                        + "Tämä on YA-integraation lähettämä automaattinen viesti";
                        
                            String subject = "YA-maksut/TYPA";

                            ex.getIn().setHeader("messageSubject", subject);
                            ex.getIn().setHeader("emailMessage", message);
                        })
                        .bean(sendEmail, "sendEmail")
                        .log("Email has been sent")

                    .otherwise()
                        .log("Files found. Continuing processing.")
                        .log("Fetching and combining the json data")
                        .bean(sftpProcessor, "fetchAllFilesFromSftpByFileName")
                        .marshal(new JacksonDataFormat())
                        .setVariable("kipa_p24_data").simple("${body}")
                        //.log("Body after fetching files :: ${body}")
                        .to("direct:maksuliikenne-controller")
                .end()
            .end()
        ;

        from("direct:poll-and-validate-file")
            .log("Processing file: ${body}")
            .setHeader("CamelFileName", simple("${body}"))
            .bean(sftpProcessor, "fetchFile")
            .log("File fecthed from kipa")
            .setVariable("originalFileName", simple("${header.CamelFileName}"))
            //.setHeader(Exchange.FILE_NAME, simple("TESTI_${header.CamelFileName}"))
            //.wireTap("direct:saveJsonData-P24")
            .process(exchange -> {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                String redisKey = "ready-to-send-verkkolevy:" + fileName;
            
                String fileContent = exchange.getIn().getBody(String.class);
            
                System.out.println("Setting the redis key :: " + redisKey);
                redisProcessor.setVerkkolevyData(redisKey, fileContent);
                        
            })
            .setHeader(Exchange.FILE_NAME, simple("${variable.originalFileName}"))
            .toD("direct:validate-json-${header.kipa_container}")
            .choice()
                .when(simple("${header.isJsonValid} == 'true'"))
                    .log("Json is valid continue processing ${header.CamelFileName}")
                    .setHeader("targetDirectory").simple("out/processed")
                    //.bean(sftpProcessor, "moveFile")
                    .unmarshal(new JacksonDataFormat())
                    .process(exchange -> {
                        Map<String, Object> fileContent = exchange.getIn().getBody(Map.class);
                        exchange.getIn().setBody(Map.of("isJsonValid", true, "fileContent", fileContent));
                    })
                    
                
                .otherwise()
                    .log("Json is not valid, ${header.CamelFileName}")
                    .log("Error message :: ${variable.error_messages}")
                    .setHeader("targetDirectory").simple("out/errors")
                    .bean(sftpProcessor, "moveFile")
                    .setHeader("messageSubject", simple("Ya-integraatio, kipa: virhe json-sanomassa, ${header.kipa_container}"))
                    .setHeader("emailRecipients", constant(JSON_ERROR_EMAIL_RECIPIENTS))
                    .to("direct:sendErrorReport")
                    .doTry()
                        .process(exchange -> {
                            String errorMessage = exchange.getVariable("error_messages", String.class);
                            throw new JsonValidationException(
                                "Invalid json file. Error messages: " + errorMessage,
                                SentryLevel.ERROR,
                            "jsonValidationError"
                            );
                        })
                    .doCatch(JsonValidationException.class)
                        .log("Caught JsonValidationException: ${exception.message}")
                        .process(exchange -> {
                            JsonValidationException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, JsonValidationException.class);
            
                            Sentry.withScope(scope -> {
                                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                                String uniqueId = UUID.randomUUID().toString(); // Generate a unique ID for the error
            
                                scope.setLevel(cause.getSentryLevel());
                                scope.setTag("error.type", cause.getTag());
                                scope.setTag("context.fileName", fileName);
                                scope.setFingerprint(Arrays.asList(uniqueId));
                                Sentry.captureException(cause);
                            });
            
                            Sentry.flush(2000);
                        })
                        
                        .process(exchange -> {
                            String errorMessage = exchange.getVariable("error_messages", String.class);
                            exchange.getIn().setBody(Map.of(
                            "isJsonValid", false,
                            "errorMessage", errorMessage
                            ));
                        })
                        
            .end()
        ;

        from("direct:validate-json-P24")
            .log("Start to validate json file")
            .process(exchange -> {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
                String[] parts = fileNameWithoutExtension.split("_");
                String lastPart = (parts.length > 0) ? parts[parts.length - 1] : "";
                exchange.getIn().setHeader("lastPart", lastPart);
            })
        
            .log("Extracted last part: ${header.lastPart}")
            .choice()
                .when(simple("${header.lastPart} == 'PT' || ${header.lastPart} == 'PT55' || ${header.lastPart} == 'TOJT'" ))
                    .log("The last part is ${header.lastPart} , validating json against schema file: " + SCHEMA_FILE_PT_PT55_TOJT)
                    .bean(jsonValidator, "validateJson(*," +  SCHEMA_FILE_PT_PT55_TOJT + ")")

                .when(simple("${header.lastPart} == 'MYK' || ${header.lastPart} == 'HKK'"))
                    .log("The last part is ${header.lastPart} , validating json against schema file: " + SCHEMA_FILE_MYK_HKK)
                    .bean(jsonValidator, "validateJson(*," +  SCHEMA_FILE_MYK_HKK + ")")
                .otherwise()
                    .log("No matching case found, skipping processing")
                    .setHeader("isJsonValid", constant(false))
                    .setVariable("error_messages", simple("Unrecognized claim type abbreviation"))
            .end()
            .log("is valid :: ${header.isJsonValid}")
        ;

        from("direct:continue-processing-P24Data")
            .unmarshal(new JacksonDataFormat())
            .aggregate(new GroupedExchangeAggregationStrategy()).constant(true)
                .completionSize(1000) 
                .completionTimeout(10000)
                .process(exchange -> {
                    //System.out.println("BODY :: " + exchange.getIn().getBody());
                    List<Exchange> combinedExchanges = exchange.getIn().getBody(List.class);
                    List<Map<String, Object>> combinedJsons = new ArrayList<>();
                    for (Exchange ex : combinedExchanges) {
                        Map<String, Object> json = ex.getIn().getBody(Map.class);
                        combinedJsons.add(json);
                    }
                    
                    exchange.getIn().setBody(combinedJsons);
                })
            
            .marshal(new JacksonDataFormat())
            //.to("file:outbox/test")
            .log("Combined jsons :: ${body}")
            .setVariable("kipa_p24_data").simple("${body}")
            .to("direct:maksuliikenne-controller")
        ;

        from("direct:saveJsonData-P24")
            .log("send json via sftp to logs")
            //.to("file:outbox/logs")
            .to("sftp:{{VERKKOLEVY_SFTP_HOST}}:22/logs?username={{VERKKOLEVY_SFTP_USER}}&password={{VERKKOLEVY_SFTP_PASSWORD}}&throwExceptionOnConnectFailed=true&strictHostKeyChecking=no")
            .log("Verkkolevy SFTP response :: ${header.CamelFtpReplyCode}  ::  ${header.CamelFtpReplyString}")   
        ;
    }
}
