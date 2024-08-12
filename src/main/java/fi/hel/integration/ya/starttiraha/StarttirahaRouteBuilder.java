package fi.hel.integration.ya.starttiraha;

import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.CsvDataFormat;

import fi.hel.integration.ya.CsvValidator;
import fi.hel.integration.ya.starttiraha.processor.StarttirahaProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StarttirahaRouteBuilder extends RouteBuilder{

    @Inject
    StarttirahaProcessor srProcessor;

    @Inject
    CsvValidator csvValidator;

    @EndpointInject("{{app.endpoints.starttiraha.sendCsv}}")
    Endpoint sendCsv;

    private final int PERSONALDATACOLUMNS = 34;
    private final int PERSONALDATAEMPTYCOLUMNS = 25;
    
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
            .multicast().stopOnException().parallelProcessing(false)
                .to("direct:sr-controller.processPersonalData")
                .to("direct:sr-controller.processPayrollTransaction")
            .end()
        ;

        // Henkilötietojen käsittely
        from("direct:sr-controller.processPersonalData")
            .log("process body :: ${body}")
            .unmarshal(new JacksonDataFormat())
            .bean(srProcessor, "createPersonalInfoMap")
            .marshal(csv)
            .setHeader(Exchange.FILE_NAME, simple("starttiraha_henkilotieto_testi_${date-with-timezone:now:Europe/Helsinki:yyyyMMddHHmmss}.csv"))
            .log("personalData body :: ${body}")
            .bean(csvValidator, "validateCsv(*," + PERSONALDATACOLUMNS + "," + PERSONALDATAEMPTYCOLUMNS + ")")
            .log("IS CSV VALID :: ${header.isCsvValid}")
            //.to("mock:processPersonalData.result")
            .to(sendCsv)
        ;

        // Palkkatapahtumien käsittely
        from("direct:sr-controller.processPayrollTransaction")
            //.log("Received message: ${body}")
            .unmarshal(new JacksonDataFormat())
            .bean(srProcessor, "createPayrollTransactionMap")
            .marshal(csv)
            .setHeader(Exchange.FILE_NAME, simple("starttiraha_palkkatapahtuma_testi_${date-with-timezone:now:Europe/Helsinki:yyyyMMddHHmmss}.csv"))
            //.log("payroll body :: ${body}")
            .to(sendCsv)
        ;

        from("direct:out.starttiraha")
            .to("file:outbox/starttiraha")
        ;
    }
}
