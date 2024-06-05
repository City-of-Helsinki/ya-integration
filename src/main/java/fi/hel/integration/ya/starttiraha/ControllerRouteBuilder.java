package fi.hel.integration.ya.starttiraha;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.CsvDataFormat;

import fi.hel.integration.ya.starttiraha.processor.Processor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ControllerRouteBuilder extends RouteBuilder{

    @Inject
    Processor processor;
    
    @Override
    public void configure() throws Exception {

        CsvDataFormat csv = new CsvDataFormat();
        // Set the delimiter to ";"
        csv.setDelimiter(";");


        // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.

        from("direct:controller")
            .setVariable("jsonData").simple("${body}")
            .to("direct:controller.processPersonalData")
            .to("file:outbox/starttiraha")
            .setBody().variable("jsonData") // restore the original data body to route
            .to("direct:controller.processPayrollTransaction")
            .to("file:outbox/starttiraha") 
        ;

        // Henkilötietojen käsittely
        from("direct:controller.processPersonalData")
            .log("process body :: ${body}")
            .unmarshal(new JacksonDataFormat())
            .bean(processor, "createPersonalInfoMap")
            .marshal(csv)
            .setHeader(Exchange.FILE_NAME, simple("starttiraha_henkilotieto_testi_${date-with-timezone:now:Europe/Helsinki:yyyyMMddHHmmss}.csv"))
        ;

        // Palkkatapahtumien käsittely
        from("direct:controller.processPayrollTransaction")
            .unmarshal(new JacksonDataFormat())
            .bean(processor, "createPayrollTransactionMap")
            .marshal(csv)
            .setHeader(Exchange.FILE_NAME, simple("starttiraha_palkkatapahtuma_testi_${date-with-timezone:now:Europe/Helsinki:yyyyMMddHHmmss}.csv"))
        ;
    }
}
