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
import fi.hel.integration.ya.exceptions.JsonValidationException;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class InStarttirahaRouteBuilder extends RouteBuilder {

    @Inject
    JsonValidator jsonValidator;

    private final String SCHEMA_FILE_SR = "schema/kipa/json_schema_SR.json";

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
            .setHeader(Exchange.FILE_NAME, simple("TESTI_${header.CamelFileName}"))
            .to("direct:saveJsonData-P22")
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

        // Reads files from the YA Kipa API
        from("sftp:{{KIPA_SFTP_HOST}}:22/{{KIPA_DIRECTORY_PATH_P22}}?username={{KIPA_SFTP_USER_P22}}"
                + "&password={{KIPA_SFTP_PASSWORD_P22}}"
                + "&strictHostKeyChecking=no"
                + "&scheduler=quartz"         
                + "&scheduler.cron={{STARTTIRAHA_QUARTZ_TIMER}}" 
                + "&antInclude=YA_p22_091_20241010*"
            )   
            .routeId("kipa-P22") 
            .autoStartup("{{STARTTIRAHA_IN_AUTOSTARTUP}}")
            .log("File fecthed from kipa")
            .setVariable("originalFileName", simple("${header.CamelFileName}"))
            .setHeader(Exchange.FILE_NAME, simple("TESTI_${header.CamelFileName}"))
            .to("direct:saveJsonData-P22")
            .setHeader(Exchange.FILE_NAME, simple("${variable.originalFileName}"))
            .to("direct:validate-json-P22")
            .choice()
                .when(simple("${header.isJsonValid} == 'true'"))
                    .log("Json is valid continue processing ${header.CamelFileName}")
                    .setVariable("kipa_dir").simple("processed")
                    .to("direct:readSFTPFileAndMove-P22")
                    .log("file moved to processed")
                    .to("direct:continue-processing-P22Data")
             
                .otherwise()
                    .log("Json is not valid, ${header.CamelFileName}")
                    .throwException(new JsonValidationException("Invalid json file", SentryLevel.ERROR, "jsonValidationError"))
                    .setVariable("kipa_dir").simple("errors")
                    .to("direct:readSFTPFileAndMove-P22")
                    .log("file moved to errors")
                    //.to("file:outbox/invalidJson")
        ;

        from("direct:validate-json-P22")
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

        from("direct:readSFTPFileAndMove-P22")
            .pollEnrich()
                .simple("sftp:{{KIPA_SFTP_HOST}}:22/{{KIPA_DIRECTORY_PATH_P22}}?username={{KIPA_SFTP_USER_P22}}&password={{KIPA_SFTP_PASSWORD_P22}}&strictHostKeyChecking=no&fileName=${headers.CamelFileName}&move=../${variable.kipa_dir}")
                .timeout(10000)
            .log("CamelFtpReplyString: ${headers.CamelFtpReplyString}")
        ;

    }
}
