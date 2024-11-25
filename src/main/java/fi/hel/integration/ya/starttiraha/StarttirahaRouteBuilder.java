package fi.hel.integration.ya.starttiraha;

import java.util.Base64;

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

    private final int PERSONALDATACOLUMNS = 29;
    private final int PERSONALDATAEMPTYCOLUMNS = 19;
    private final int PAYROLLDATACOLUMNS = 8;
    private final int PAYROLLDATAEMPTYCOLUMNS = 3;
    
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

        from("direct:starttiraha-controller")
            .multicast().stopOnException().parallelProcessing(false)
                .to("direct:processPersonalData")
                .to("direct:processPayrollTransaction")
            .end()
        ;

        // Henkilötietojen käsittely
        from("direct:processPersonalData")
            .log("process body :: ${body}")
            .unmarshal(new JacksonDataFormat())
            .bean(srProcessor, "createPersonalInfoMap")
            .marshal(csv)
            .setHeader(Exchange.FILE_NAME, simple("starttiraha_henkilotieto_testi_${date-with-timezone:now:Europe/Helsinki:yyyyMMddHHmmss}.csv"))
            //.log("personalData body :: ${body}")
            .setHeader("columns", constant(PERSONALDATACOLUMNS))
            .setHeader("emptyColumns", constant(PERSONALDATAEMPTYCOLUMNS))
            .bean(csvValidator, "validateCsv(*)")
            .log("IS CSV VALID :: ${header.isCsvValid}")
            .choice()
                .when(simple("${header.isCsvValid} == 'true'"))
                    .log("csv is valid")
                    //.to("mock:processPersonalData.result")
                    .to(sendCsv)
                    //.log("personal data csv :: ${body}")
                .otherwise()
                    .log("CSV is not valid, ${header.CamelFileName}")
            
        ;

        // Palkkatapahtumien käsittely
        from("direct:processPayrollTransaction")
            //.log("Received message: ${body}")
            .unmarshal(new JacksonDataFormat())
            .bean(srProcessor, "createPayrollTransactionMap")
            .marshal(csv)
            .setHeader(Exchange.FILE_NAME, simple("starttiraha_palkkatapahtuma_testi_${date-with-timezone:now:Europe/Helsinki:yyyyMMddHHmmss}.csv"))
            .setHeader("columns", constant(PAYROLLDATACOLUMNS))
            .setHeader("emptyColumns", constant(PAYROLLDATAEMPTYCOLUMNS))
            .bean(csvValidator, "validateCsv(*)")
            //.log("payroll body :: ${body}")
            .log("IS CSV VALID :: ${header.isCsvValid}")
            .choice()
                .when(simple("${header.isCsvValid} == 'true'"))
                    .log("csv is valid")
                    //.to("mock:processPersonalData.result")
                    //.log("payroll data csv :: ${body}")
                    .to(sendCsv)
                .otherwise()
                    .log("CSV is not valid, ${header.CamelFileName}")   
        ;

        from("direct:out.starttiraha")
            .log("Sending the csv file to AHR")
            //.to("file:outbox/starttiraha")
            .setHeader("privateKeyEncoded", simple("{{AHR_SFTP_PRIVATEKEY}}"))
            .process(ex -> {
                String privateKeyEncoded = ex.getIn().getHeader("privateKeyEncoded", String.class);
                byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyEncoded);
                ex.getIn().setHeader("privateKey", privateKeyBytes);
                
            })
            .to("sftp:{{ahr_sftp_host}}:22/In?username={{AHR_SFTP_USER}}&privateKey=${header.privateKey}&throwExceptionOnConnectFailed=true&strictHostKeyChecking=no")
            .log("SFTP response :: ${header.CamelFtpReplyCode}  ::  ${header.CamelFtpReplyString}")
        ;
    }
}
