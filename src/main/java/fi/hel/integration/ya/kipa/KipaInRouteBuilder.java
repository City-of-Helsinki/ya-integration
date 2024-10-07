package fi.hel.integration.ya.kipa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;

import fi.hel.integration.ya.JsonValidator;
import fi.hel.integration.ya.maksuliikenne.processor.MaksuliikenneProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KipaInRouteBuilder extends RouteBuilder{

    @Inject
    JsonValidator jsonValidator;

    @Inject
    MaksuliikenneProcessor mlProcessor;

    private final String SCHEMA_FILE_PT_PT55_TOJT = "schema/kipa/json_schema_PT_PT55_TOJT.json";
    private final String SCHEMA_FILE_MYK_HKK = "schema/kipa/json_schema_MYK_HKK.json";
    private final String SCHEMA_FILE_SR = "schema/kipa/json_schema_SR.json";


    @Override
    public void configure() throws Exception {

       // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.


        // This route is for local development and testing
        // The route is triggered by dropping json file/files into folder inbox/kipa/P24
        from("file:inbox/kipa/P24")
            .log("body :: ${body}")
            .to("direct:validate-json")
            .choice()
                .when(simple("${header.isJsonValid} == 'true'"))
                    .log("Json is valid continue processing ${header.CamelFileName}")
                    .to("direct:continue-processing-P24Data")
                .otherwise()
                    .log("Json is not valid, ${header.CamelFileName}")
                    .to("file:outbox/invalidJson")
        ;

        // This route is for local development and testing
        // The route is triggered by dropping json file/files into folder inbox/kipa/P22
        from("file:inbox/kipa/P22")
            .log("body :: ${body}")
            .to("direct:validate-json")
            .choice()
                .when(simple("${header.isJsonValid} == 'true'"))
                    .log("Json is valid continue processing ${header.CamelFileName}")
                    .to("direct:continue-processing-P22Data")
                .otherwise()
                    .log("Json is not valid, ${header.CamelFileName}")
                    .to("file:outbox/invalidJson")
        ;

        // Reads files from the YA Kipa API
        from("sftp:{{KIPA_SFTP_HOST}}:22/{{KIPA_DIRECTORY_PATH_P24}}?username={{KIPA_SFTP_USER_P24}}"
                + "&password={{KIPA_SFTP_PASSWORD_P24}}"
                + "&strictHostKeyChecking=no"
                + "&scheduler=quartz"         
                + "&scheduler.cron={{MAKSULIIKENNE_QUARTZ_TIMER}}" 
                + "&antInclude=YA_20240424*"
            )   
            .routeId("kipa-P24") 
            .autoStartup("{{MAKSULIIKENNE_IN_AUTOSTARTUP}}")
            .to("direct:validate-json")
            .choice()
                .when(simple("${header.isJsonValid} == 'true'"))
                    .log("Json is valid continue processing ${header.CamelFileName}")
                    .setVariable("kipa_dir").simple("processed")
                    .to("direct:readSFTPFileAndMove")
                    .log("file moved to processed")
                    .to("direct:continue-processing-P24Data")
             
                .otherwise()
                    .log("Json is not valid, ${header.CamelFileName}")
                    .setVariable("kipa_dir").simple("errors")
                    .to("direct:readSFTPFileAndMove")
                    .log("file moved to errors")
                    //.to("file:outbox/invalidJson")
        ;

        from("direct:validate-json")
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
                    .log("The last part is PT, performing PT-specific processing")
                    .bean(jsonValidator, "validateJson(*," +  SCHEMA_FILE_PT_PT55_TOJT + ")")

                .when(simple("${header.lastPart} == 'MYK' || ${header.lastPart} == 'HKK'"))
                    .log("The last part is MYK, performing MYK-specific processing")
                    .bean(jsonValidator, "validateJson(*," +  SCHEMA_FILE_MYK_HKK + ")")
                .when(simple("${header.lastPart} == 'SR'"))
                    .log("The last part is SR, performing SR-specific processing")
                    .bean(jsonValidator, "validateJson(*," +  SCHEMA_FILE_SR + ")")
                .otherwise()
                    .log("No matching case found, skipping processing")
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
            .to("direct:ml-controller")
        ;

        from("direct:continue-processing-P22Data")
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
            .to("direct:sr-controller")
        ;

        from("direct:readSFTPFileAndMove")
            .pollEnrich()
                .simple("sftp:{{KIPA_SFTP_HOST}}:22/{{KIPA_DIRECTORY_PATH_P24}}?username={{KIPA_SFTP_USER_P24}}&password={{KIPA_SFTP_PASSWORD_P24}}&strictHostKeyChecking=no&fileName=${headers.CamelFileName}&move=${file:parent}/../out/${variable.kipa_dir}/${file:onlyname}")
                .timeout(10000)
            .log("CamelFtpReplyString: ${headers.CamelFtpReplyString}")
        ;

        from("timer://testP24Route?repeatCount=1")
            .autoStartup("{{MAKSULIIKENNE_TESTROUTE_AUTOSTARTUP}}")
            .log("Starting kipa P24 test route")
            //.log("test secret :: " + testSecret)
            .setHeader("hostname").simple("{{KIPA_SFTP_HOST}}")
            .setHeader("username").simple("{{KIPA_SFTP_USER_P24}}")
            .setHeader("password").simple("{{KIPA_SFTP_PASSWORD_P24}}")
            .setHeader("directoryPath").simple("{{KIPA_DIRECTORY_PATH_P24}}")
            .to("direct:fetchFileNamesFromKipa")
            .to("direct:fetchDirectoriesFromKipa")
        ;

        from("direct:fetchFileNamesFromKipa")
            .bean(mlProcessor, "getAllSFTPFileNames(*)")
            .log("File names :: ${body}")
        ;

        from("direct:fetchDirectoriesFromKipa")
            .bean(mlProcessor, "getAllSFTPDirectories")
            .log("Directories :: ${body}")
        ;
    }
}
