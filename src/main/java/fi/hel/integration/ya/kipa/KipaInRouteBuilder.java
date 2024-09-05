package fi.hel.integration.ya.kipa;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;

import fi.hel.integration.ya.JsonValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KipaInRouteBuilder extends RouteBuilder{

    @Inject
    JsonValidator jsonValidator;

    private final String SCHEMA_FILE_PT_PT55_TOJT = "src/main/resources/schema/kipa/json_schema_PT_PT55_TOJT.json";
    private final String SCHEMA_FILE_MYK_HKK = "src/main/resources/schema/kipa/json_schema_MYK_HKK.json";

    @Override
    public void configure() throws Exception {

       // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.


        from("file:inbox/kipa")
            .log("body :: ${body}")
            .log("filename :: ${header.CamelFileName}")
            .log("filename only :: ${header.CamelFileNameOnly}")
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
                .otherwise()
                    .log("No matching case found, skipping processing")
            .end()
            .log("is valid :: ${header.isJsonValid}")
            .choice()
                .when(simple("${header.isJsonValid} == 'true'"))
                    .log("Json is valid continue processing ${header.CamelFileName}")
                    .to("file:outbox/validJson")
                .otherwise()
                    .log("Json is not valid, ${header.CamelFileName}")
                    .to("file:outbox/invalidJson")
        ;

    }

}
