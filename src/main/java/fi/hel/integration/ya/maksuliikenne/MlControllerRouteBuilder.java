package fi.hel.integration.ya.maksuliikenne;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.JacksonXMLDataFormat;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import fi.hel.integration.ya.Utils;
import fi.hel.integration.ya.maksuliikenne.models.pain.Document;
import fi.hel.integration.ya.maksuliikenne.processor.MlProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MlControllerRouteBuilder extends RouteBuilder {

    @Inject
    MlProcessor mlProcessor;

    @Inject
    Utils utils;

    private final String SCHEMA_FILE = "{{maksuliikenne.pain.schema.file}}";

    @Override
    public void configure() throws Exception {

       // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.

        from("direct:ml-controller")
            .unmarshal(new JacksonDataFormat())
            .bean(mlProcessor, "mapPaymentTransactions")
            .marshal().jacksonXml(Document.class)
            .convertBodyTo(String.class)
            .setBody().groovy("'{{maksuliikenne.xml.declaration}}' + body")
            .bean(utils, "validateXml(*," +  SCHEMA_FILE + ")")
            .log("is valid :: ${header.isXmlValid}")
            .setHeader(Exchange.FILE_NAME, simple("testi.xml"))
            .to("file:outbox/maksuliikenne")
        
        ;
    }
}
