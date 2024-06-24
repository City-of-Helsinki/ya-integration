package fi.hel.integration.ya.starttiraha;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.CsvDataFormat;

import fi.hel.integration.ya.starttiraha.processor.SrProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SrControllerRouteBuilder extends RouteBuilder{

    @Inject
    SrProcessor srProcessor;
    
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

        from("direct:sr-controller")
            .setVariable("jsonData").simple("${body}")
            .to("direct:sr-controller.processPersonalData")
            .to("file:outbox/starttiraha")
            .setBody().variable("jsonData") // restore the original data body to route
            .to("direct:sr-controller.processPayrollTransaction")
            .to("file:outbox/starttiraha") 
        ;

        // Henkilötietojen käsittely
        from("direct:sr-controller.processPersonalData")
            .log("process body :: ${body}")
            .unmarshal(new JacksonDataFormat())
            .bean(srProcessor, "createPersonalInfoMap")
            .marshal(csv)
            .setHeader(Exchange.FILE_NAME, simple("starttiraha_henkilotieto_testi_${date-with-timezone:now:Europe/Helsinki:yyyyMMddHHmmss}.csv"))
        ;

        // Palkkatapahtumien käsittely
        from("direct:sr-controller.processPayrollTransaction")
            .unmarshal(new JacksonDataFormat())
            .bean(srProcessor, "createPayrollTransactionMap")
            .marshal(csv)
            .setHeader(Exchange.FILE_NAME, simple("starttiraha_palkkatapahtuma_testi_${date-with-timezone:now:Europe/Helsinki:yyyyMMddHHmmss}.csv"))
        ;
    }
}
