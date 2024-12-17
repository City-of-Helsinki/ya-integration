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
import fi.hel.integration.ya.RedisLockRoutePolicy;
import fi.hel.integration.ya.RedisProcessor;
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

    private final String SCHEMA_FILE_PT_PT55_TOJT = "schema/kipa/json_schema_PT_PT55_TOJT.json";
    private final String SCHEMA_FILE_MYK_HKK = "schema/kipa/json_schema_MYK_HKK.json";

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
            .log("Body before validating :: ${body}")
            .to("direct:validate-json-P24")
            .choice()
                .when(simple("${header.isJsonValid} == 'true'"))
                    .log("Body after validating :: ${body}")
                    .log("Json is valid continue processing ${header.CamelFileName}")
                    .unmarshal(new JacksonDataFormat())
                    .process(exchange -> {
                        // Get the file content
                        Map<String,Object> fileContent = exchange.getIn().getBody(Map.class);
                        System.out.println("file content :: " + fileContent);
                        ArrayList<Map<String,Object>> combinedJsons = exchange.getVariable("combinedJsons", ArrayList.class);

                        combinedJsons.add(fileContent);
                        //exchange.setVariable("combinedJsons", combinedJsons);
                    })
                    .log("Variable jsons :: ${variable.combinedJsons}")
                    //.to("direct:continue-processing-P24Data")
                .otherwise()
                    .log("Json is not valid, ${header.CamelFileName}")
                    .log("Error message :: ${header.jsonValidationErrors}")
                    .process(exchange -> {
                        String errorMessages = exchange.getIn().getHeader("jsonValidationErrors", String.class);
                        throw new JsonValidationException(
                            "Invalid json file. Error messages: " + errorMessages,
                            SentryLevel.ERROR,
                            "jsonValidationError"
                        );
                    })
            .end()
            .process(exchange -> {
                List<Map<String, Object>> combinedJsons = exchange.getVariable("combinedJsons", List.class);
                exchange.getIn().setBody(combinedJsons);
            })
            .log("Body before continue processing :: ${body}")
            .to("direct:continue-processing-P24Data")
                //.to("file:outbox/invalidJson")
        ;

        from("timer://kipa_P24?repeatCount=1")
            .routeId("kipa-P24")
            .autoStartup("{{MAKSULIIKENNE_IN_AUTOSTARTUP}}")
            .log("Start route to fetch files from kipa P24")
            .setHeader("hostname").simple("{{KIPA_SFTP_HOST}}")
            .setHeader("username").simple("{{KIPA_SFTP_USER_P24}}")
            .setHeader("password").simple("{{KIPA_SFTP_PASSWORD_P24}}")
            .setHeader("directoryPath").simple("{{KIPA_DIRECTORY_PATH_P24}}")
            .setHeader("filePrefix", constant("YA_p24_091_20241209105808"))
            .setHeader("filePrefix2", constant("YA_p23_091_20241209110911_091_ATVK"))
            .log("Fetching file names from Kipa")
            .bean("sftpProcessor", "getAllSFTPFileNames")
            .log("Fetching and combining the json data")
            .bean(sftpProcessor, "fetchAllFilesFromSftpByFileName")
            .marshal(new JacksonDataFormat())
            .setVariable("kipa_p24_data").simple("${body}")
            .log("Body before continue processing :: ${body}")
            //.to("direct:maksuliikenne-controller")
        ;

        from("direct:poll-and-validate-file")
            .log("Processing file: ${body}") // Log each file name
            .setHeader("CamelFileName", simple("${body}")) // Set the file name for pollEnrich
            .pollEnrich()
                .simple("sftp://{{KIPA_SFTP_HOST}}/{{KIPA_DIRECTORY_PATH_P24}}?username={{KIPA_SFTP_USER_P24}}&password={{KIPA_SFTP_PASSWORD_P24}}&fileName=${header.CamelFileName}") 
                .timeout(60000)
            .log("File fecthed from kipa")
            .setVariable("originalFileName", simple("${header.CamelFileName}"))
            .setHeader(Exchange.FILE_NAME, simple("TESTI_${header.CamelFileName}"))
            //.to("direct:saveJsonData-P24")
            .setHeader(Exchange.FILE_NAME, simple("${variable.originalFileName}"))
            .to("direct:validate-json-P24")
            .choice()
                .when(simple("${header.isJsonValid} == 'true'"))
                    .log("Json is valid continue processing ${header.CamelFileName}")
                    .setVariable("kipa_dir").simple("processed")
                    //.to("direct:readSFTPFileAndMove-P24")
                    //.log("file moved to processed")
                    .unmarshal(new JacksonDataFormat())
                    .process(exchange -> {
                        Map<String, Object> fileContent = exchange.getIn().getBody(Map.class);
                        exchange.getIn().setBody(Map.of("isJsonValid", true, "fileContent", fileContent));
                    })
    
            .otherwise()
                .log("Json is not valid, ${header.CamelFileName}")
                .log("Error message :: ${header.jsonValidationErrors}")
                .doTry()
                    .process(exchange -> {
                        String errorMessage = exchange.getIn().getHeader("jsonValidationErrors", String.class);
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
            
                        // Send error to Sentry explicitly
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
                    .setVariable("kipa_dir").simple("errors")
                    //.wireTap("direct:readSFTPFileAndMove-P24")
                    //.log("file moved to errors")
                    .process(exchange -> {
                        String errorMessage = exchange.getIn().getHeader("jsonValidationErrors", String.class);
                        exchange.getIn().setBody(Map.of(
                        "isJsonValid", false,
                        "errorMessage", errorMessage
                        ));
                    })
                    //.to("file:outbox/invalidJson")
        ;

        // Reads files from the YA Kipa API
        /* from("sftp:{{KIPA_SFTP_HOST}}:22/{{KIPA_DIRECTORY_PATH_P24}}?username={{KIPA_SFTP_USER_P24}}"
                + "&password={{KIPA_SFTP_PASSWORD_P24}}"
                + "&strictHostKeyChecking=no"
                + "&scheduler=quartz"         
                + "&scheduler.cron={{MAKSULIIKENNE_QUARTZ_TIMER}}" 
                + "&antInclude=YA_p24_091_20241209105802_091_PT55*"
            )   
            //.routeId("kipa-P24") 
            .autoStartup("{{MAKSULIIKENNE_IN_AUTOSTARTUP}}")
            //.routePolicy(new RedisLockRoutePolicy(redisProcessor, LOCK_KEY, 300))
            .log("File fecthed from kipa")
            .setVariable("originalFileName", simple("${header.CamelFileName}"))
            .setHeader(Exchange.FILE_NAME, simple("TESTI_${header.CamelFileName}"))
            //.to("direct:saveJsonData-P24")
            .setHeader(Exchange.FILE_NAME, simple("${variable.originalFileName}"))
            //.log("Body after saving the json to logs :: ${body}")
            .to("direct:validate-json-P24")
            .choice()
                .when(simple("${header.isJsonValid} == 'true'"))
                    .log("Json is valid continue processing ${header.CamelFileName}")
                    .setVariable("kipa_dir").simple("processed")
                    .to("direct:readSFTPFileAndMove-P24")
                    .log("file moved to processed")
                    .to("direct:continue-processing-P24Data")
             
            .otherwise()
                .log("Json is not valid, ${header.CamelFileName}")
                .log("Error message :: ${header.jsonValidationErrors}")
                .process(exchange -> {
                    String errorMessages = exchange.getIn().getHeader("jsonValidationErrors", String.class);
                    throw new JsonValidationException(
                        "Invalid json file. Error messages: " + errorMessages,
                        SentryLevel.ERROR,
                        "jsonValidationError"
                    );
                })
                .setVariable("kipa_dir").simple("errors")
                //.to("direct:readSFTPFileAndMove-P24")
                .log("file moved to errors")
                //.to("file:outbox/invalidJson")
        ;  */

        from("direct:validate-json-P24")
            .log("Start to validate json file")
            .process(exchange -> {
                // Get the filename from the header
                String fileName = exchange.getIn().getHeader("CamelFileNameOnly", String.class);
        
                // Remove the file extension to get the base name
                String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
        
                // Split the filename by underscore (_)
                String[] parts = fileNameWithoutExtension.split("_");
        
                // Ensure the parts array is not empty and get the last part
                String lastPart = (parts.length > 0) ? parts[parts.length - 1] : "";
        
                // Set the last part in the exchange header
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
                    .setHeader("jsonValidationErrors", simple("Unrecognized claim type abbreviation"))
            .end()
            .log("is valid :: ${header.isJsonValid}")
        ;

        from("direct:continue-processing-P24Data")
            //.unmarshal(new JacksonDataFormat())
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

        from("direct:readSFTPFileAndMove-P24")
            .pollEnrich()
                .simple("sftp:{{KIPA_SFTP_HOST}}:22/{{KIPA_DIRECTORY_PATH_P24}}?username={{KIPA_SFTP_USER_P24}}&password={{KIPA_SFTP_PASSWORD_P24}}&strictHostKeyChecking=no&fileName=${headers.CamelFileName}&move=../${variable.kipa_dir}")
                .timeout(10000)
            .log("CamelFtpReplyString: ${headers.CamelFtpReplyString}")
        ;

        from("direct:saveJsonData-P24")
            .log("send json via sftp to logs")
            //.to("file:outbox/logs")
            .to("sftp:{{VERKKOLEVY_SFTP_HOST}}:22/logs?username={{VERKKOLEVY_SFTP_USER}}&password={{VERKKOLEVY_SFTP_PASSWORD}}&throwExceptionOnConnectFailed=true&strictHostKeyChecking=no")
            .log("SFTP response :: ${header.CamelFtpReplyCode}  ::  ${header.CamelFtpReplyString}")   
        ;
    }
}
