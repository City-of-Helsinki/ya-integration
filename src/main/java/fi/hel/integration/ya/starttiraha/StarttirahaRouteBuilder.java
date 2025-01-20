package fi.hel.integration.ya.starttiraha;

import java.util.Arrays;
import java.util.UUID;

import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.model.dataformat.CsvDataFormat;

import fi.hel.integration.ya.CsvValidator;
import fi.hel.integration.ya.SftpProcessor;
import fi.hel.integration.ya.exceptions.JsonValidationException;
import fi.hel.integration.ya.exceptions.CsvValidationException;
import fi.hel.integration.ya.starttiraha.processor.StarttirahaProcessor;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StarttirahaRouteBuilder extends RouteBuilder{

    @Inject
    StarttirahaProcessor srProcessor;

    @Inject
    CsvValidator csvValidator;

    @Inject
    SftpProcessor sftpProcessor;

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

            onException(CsvValidationException.class)
            .handled(true)
            .process(exchange -> {
                CsvValidationException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, CsvValidationException.class);

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
            .log("CsvValidationException occurred: ${exception.message}")
        ;

        from("direct:starttiraha-controller")
            .multicast().stopOnException().parallelProcessing(false)
                .to("direct:processPersonalData")
                .to("direct:processPayrollTransaction")
            .end()
            .log("All starttiraha data processed")
            .setBody().variable("kipa_p22_data")
            .unmarshal(new JacksonDataFormat())
            .setHeader("hostname").simple("{{KIPA_SFTP_HOST}}")
            .setHeader("username").simple("{{KIPA_SFTP_USER_P22}}")
            .setHeader("password").simple("{{KIPA_SFTP_PASSWORD_P22}}")
            .setHeader("directoryPath").simple("{{KIPA_DIRECTORY_PATH_P22}}")
            .setHeader("targetDirectory").simple("out/processed")
            //.log("Files to be moved to processed dir :: ${body}")
            .log("Moving files to kipa ${header.targetDirectory} folder")
            //.bean(sftpProcessor, "moveFiles")
            .setBody().variable("invalidFiles")
            .choice()
                .when(simple("${body} == null || ${body.size()} == 0"))
                    .log("There was no invalid json files")
                .otherwise()
                    .setHeader("targetDirectory").simple("out/errors")
                    .log("Moving files to kipa ${header.targetDirectory} folder")
                    //.log("Files to be moved to errors dir :: ${body}")
                    .bean(sftpProcessor, "moveFiles")
            .end()
        ;

        // Henkilötietojen käsittely
        from("direct:processPersonalData")
            .log("process personal data")
            .unmarshal(new JacksonDataFormat())
            .bean(srProcessor, "createPersonalInfoMap")
            .marshal(csv)
            .process(exchange -> {
                String body = exchange.getIn().getBody(String.class);
                // Prepend a UTF-8 Byte Order Mark (BOM) to the body.
                // This ensures that programs like Excel correctly interpret the file as UTF-8 encoded,
                // avoiding issues with special characters (e.g., ä, ö, ü) being displayed incorrectly.
                exchange.getIn().setBody("\uFEFF" + body);
            })
            .setHeader(Exchange.FILE_NAME, simple("starttiraha_henkilotieto_${date-with-timezone:now:Europe/Helsinki:yyyyMMddHHmmss}.csv"))
            //.log("personalData body :: ${body}")
            .setHeader("columns", constant(PERSONALDATACOLUMNS))
            .setHeader("emptyColumns", constant(PERSONALDATAEMPTYCOLUMNS))
            .bean(csvValidator, "validateCsv(*)")
            .log("IS CSV VALID :: ${header.isCsvValid}")
            .to("mock:processPersonalData.result")
            .choice()
                .when(simple("${header.isCsvValid} == 'true'"))
                    .log("csv is valid")
                    //.log("personal data csv :: ${body}")
                    //.to("file:outbox/starttiraha")
                    .to(sendCsv) 
                .otherwise()
                    .log("CSV is not valid, ${header.CamelFileName}")
                    .throwException(new CsvValidationException("Invalid csv file", SentryLevel.ERROR, "csvValidationError"))
            
        ;

        // Palkkatapahtumien käsittely
        from("direct:processPayrollTransaction")
            .log("process payroll transaction")
            .unmarshal(new JacksonDataFormat())
            .bean(srProcessor, "createPayrollTransactionMap")
            .marshal(csv)
            .process(exchange -> {
                String body = exchange.getIn().getBody(String.class);
                // Prepend a UTF-8 Byte Order Mark (BOM) to the body.
                // This ensures that programs like Excel correctly interpret the file as UTF-8 encoded,
                // avoiding issues with special characters (e.g., ä, ö, ü) being displayed incorrectly.
                exchange.getIn().setBody("\uFEFF" + body);
            })
            .setHeader(Exchange.FILE_NAME, simple("starttiraha_palkkatapahtuma_${date-with-timezone:now:Europe/Helsinki:yyyyMMddHHmmss}.csv"))
            .setHeader("columns", constant(PAYROLLDATACOLUMNS))
            .setHeader("emptyColumns", constant(PAYROLLDATAEMPTYCOLUMNS))
            .bean(csvValidator, "validateCsv(*)")
            //.log("payroll body :: ${body}")
            .log("IS CSV VALID :: ${header.isCsvValid}")
            .to("mock:processPayrollTransaction.result")
            .choice()
                .when(simple("${header.isCsvValid} == 'true'"))
                    .log("csv is valid")
                    //.log("payroll data csv :: ${body}")
                    //.to("file:outbox/starttiraha")
                    .to(sendCsv)
                .otherwise()
                    .log("CSV is not valid, ${header.CamelFileName}")   
        ;

        from("direct:out.starttiraha")
            .log("Sending the csv file to AHR")
            //.to("file:outbox/starttiraha")
            .setHeader("hostname").simple("{{AHR_SFTP_HOST}}")
            .setHeader("username").simple("{{AHR_SFTP_USER}}")
            .setHeader("privateKey").simple("{{AHR_SFTP_PRIVATEKEY}}")
            .setHeader("directoryPath").simple("{{AHR_DIRECTORY_PATH_IN}}")
            .bean(srProcessor, "writeFileSftp(*)")
            .log("SFTP response :: ${header.CamelFtpReplyCode}  ::  ${header.CamelFtpReplyString}")
        ;
    }
}
