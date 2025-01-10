package fi.hel.integration.ya.starttiraha;

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
import fi.hel.integration.ya.SftpProcessor;
import fi.hel.integration.ya.exceptions.JsonValidationException;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class InStarttirahaRouteBuilder extends RouteBuilder {

    @Inject
    JsonValidator jsonValidator;

    @Inject
    SftpProcessor sftpProcessor;

    @Inject
    RedisProcessor redisProcessor;

    private final String SCHEMA_FILE_SR = "schema/kipa/json_schema_SR.json";

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
        // The route is triggered by dropping json file/files into folder inbox/kipa/P22
        from("file:inbox/kipa/P22")
            //.log("body :: ${body}")
            .setVariable("originalFileName", simple("${header.CamelFileName}"))
            //.setHeader(Exchange.FILE_NAME, simple("TESTI_${header.CamelFileName}"))
            .process(exchange -> {
                String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
                String redisKey = "ready-to-send-verkkolevy:" + fileName;
        
                String fileContent = exchange.getIn().getBody(String.class);
        
                System.out.println("Setting the redis key :: " + redisKey);
                redisProcessor.setVerkkolevyData(redisKey, fileContent);
                    
            })
            //.to("direct:saveJsonData-P22")
            .setHeader(Exchange.FILE_NAME, simple("${variable.originalFileName}"))
            .log("Validating json file :: ${header.CamelFileName}")
            .to("direct:validate-json-P22")
            .choice()
                .when(simple("${header.isJsonValid} == 'true'"))
                    .log("Json is valid continue processing ${header.CamelFileName}")
                    .to("direct:continue-processing-P22Data")
                .otherwise()
                    .log("Json is not valid, ${header.CamelFileName}")
                    .throwException(new JsonValidationException("Invalid json file", SentryLevel.ERROR, "jsonValidationError"))
                    .to("file:outbox/invalidJson")

        ;

        from("{{STARTTIRAHA_QUARTZ_TIMER}}")
            .routeId("kipa-P22")
            .autoStartup("{{STARTTIRAHA_IN_AUTOSTARTUP}}")
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
                .log("Start route to fetch files from kipa P22")
                .setHeader("hostname").simple("{{KIPA_SFTP_HOST}}")
                .setHeader("username").simple("{{KIPA_SFTP_USER_P22}}")
                .setHeader("password").simple("{{KIPA_SFTP_PASSWORD_P22}}")
                .setHeader("directoryPath").simple("{{KIPA_DIRECTORY_PATH_P22}}")
                .setHeader("kipa_container", simple("P22"))
                //.setHeader("filePrefix", constant("YA_p22_091_202408"))
                //.setHeader("filePrefix2", constant("YA_p22_091_20240611103500_0_SR.json"))
                .log("Fetching file names from Kipa")
                .bean("sftpProcessor", "getAllSFTPFileNames")
                .choice()
                    .when(simple("${body} == null || ${body.size()} == 0"))
                        .log("No files found in SFTP.")
                    .otherwise()
                        .log("Files found. Continuing processing.")
                        .log("Fetching and combining the json data")
                        .bean(sftpProcessor, "fetchAllFilesFromSftpByFileName")
                        .marshal(new JacksonDataFormat())
                        .setVariable("kipa_p22_data").simple("${body}")
                        //.log("Body before continue processing :: ${body}")
                        .to("direct:starttiraha-controller")
                .end()
            .end()
        ;

        from("direct:validate-json-P22")
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
                .when(simple("${header.lastPart} == 'SR'"))
                    .log("The last part is ${header.lastPart} , validating json against schema file: " + SCHEMA_FILE_SR)
                    .bean(jsonValidator, "validateJson(*," +  SCHEMA_FILE_SR + ")")
                .otherwise()
                    .log("There is something wrong with the file; the TOJ abbreviation is either incorrect or cannot be resolved, skipping processing")
            .end()
            .log("is valid :: ${header.isJsonValid}")
        ;

        from("direct:continue-processing-P22Data")
            .unmarshal(new JacksonDataFormat())
            .aggregate(new GroupedExchangeAggregationStrategy()).constant(true)
                .completionSize(1000) 
                .completionTimeout(50000)
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
            .setVariable("kipa_p22_data").simple("${body}")
            .to("direct:starttiraha-controller")
        ;

        from("direct:saveJsonData-P22")
            .log("send json via sftp")
            //.to("file:outbox/logs")
            .to("sftp:{{VERKKOLEVY_SFTP_HOST}}:22/logs?username={{VERKKOLEVY_SFTP_USER}}&password={{VERKKOLEVY_SFTP_PASSWORD}}&throwExceptionOnConnectFailed=true&strictHostKeyChecking=no")
            .log("SFTP response :: ${header.CamelFtpReplyCode}  ::  ${header.CamelFtpReplyString}")   
        ;

    }
}
