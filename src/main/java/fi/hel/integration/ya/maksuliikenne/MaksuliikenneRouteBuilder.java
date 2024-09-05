package fi.hel.integration.ya.maksuliikenne;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.JacksonXMLDataFormat;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import fi.hel.integration.ya.Utils;
import fi.hel.integration.ya.XmlValidator;
import fi.hel.integration.ya.maksuliikenne.models.pain.Document;
import fi.hel.integration.ya.maksuliikenne.processor.MaksuliikenneProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MaksuliikenneRouteBuilder extends RouteBuilder {

    @Inject
    MaksuliikenneProcessor mlProcessor;

    @Inject
    Utils utils;

    @Inject
    XmlValidator xmlValidator;

    private final String SCHEMA_FILE = "{{maksuliikenne.pain.schema.file}}";

    @Override
    public void configure() throws Exception {

       // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.

        from("direct:ml-controller")
            .log("json body :: ${body}")
            .to("file:outbox/test")
            .to("direct:mapPaymentTransactions")
            .bean(xmlValidator, "validateXml(*," +  SCHEMA_FILE + ")")
            .log("is valid :: ${header.isXmlValid}")
            .setHeader(Exchange.FILE_NAME, simple("testi.xml"))
            .to("mock:sendMaksuliikenneXml")
            .to("file:outbox/maksuliikenne")
        
        ;

        from("direct:mapPaymentTransactions")
            .unmarshal(new JacksonDataFormat())
            .bean(mlProcessor, "mapPaymentTransactions")
            .marshal().jacksonXml(Document.class)
            //.log("xml body :: ${body}")
            .convertBodyTo(String.class)
            .setBody().groovy("'{{maksuliikenne.xml.declaration}}' + body")
            .to("mock:mapPaymentTransactions.result")
        ;
    }
}