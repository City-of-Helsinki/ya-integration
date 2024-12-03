package fi.hel.integration.ya.maksuliikenne;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;

import fi.hel.integration.ya.Utils;
import fi.hel.integration.ya.XmlValidator;
import fi.hel.integration.ya.exceptions.XmlValidationException;
import fi.hel.integration.ya.maksuliikenne.models.pain.Document;
import fi.hel.integration.ya.maksuliikenne.processor.MaksuliikenneProcessor;
import fi.hel.integration.ya.maksuliikenne.processor.SendEmail;
import io.sentry.Sentry;
import io.sentry.SentryLevel;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MaksuliikenneRouteBuilder extends RouteBuilder {

    @Inject
    MaksuliikenneProcessor mlProcessor;

    @Inject
    SendEmail sendEmail;

    @Inject
    Utils utils;

    @Inject
    XmlValidator xmlValidator;

    private final String SCHEMA_FILE = "schema/banking/pain.001.001.03.xsd";
    private final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private final String FILE_NAME_PREFIX = "{{MAKSULIIKENNE_BANKING_FILENAMEPREFIX}}";

    @Override
    public void configure() throws Exception {

       // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.

        onException(XmlValidationException.class)
            .handled(true)
            .process(exchange -> {
                XmlValidationException cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, XmlValidationException.class);

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
            .log("XmlValidationException occurred: ${exception.message}")
        ;

        from("direct:maksuliikenne-controller")
            //.to("file:outbox/test")
            .to("direct:mapPaymentTransactions")
            .bean(xmlValidator, "validateXml(*," +  SCHEMA_FILE + ")")
            .log("xml is valid :: ${header.isXmlValid}")
            .setHeader(Exchange.FILE_NAME, simple(FILE_NAME_PREFIX + "${date:now:yyyyMMddHHmmss}.xml"))
            .to("mock:sendMaksuliikenneXml")
            //.to("file:outbox/maksuliikenne")
            //.log("Pain xml :: ${body}")
            .choice()
                .when(simple("${header.isXmlValid} == 'true'"))
                    .log("XML is valid, sending the file to banking ${header.CamelFileName}")
                    //.to("direct:out-banking")
                    .setHeader("CamelFtpReplyString").simple("OK")
                    .choice()
                        .when(simple("${header.CamelFtpReplyString} == 'OK'"))
                            .log("The pain xml has been sent to Banking")
                            //.to("direct:sendMaksuliikenneReportEmail")
                            // Restore the Kipa data to the route and direct it to the accounting mapping
                            .setBody().variable("kipa_p24_data")
                            .log("kirjanpito data :: ${body}")
                            //.to("direct:kirjanpito.controller")
                        .otherwise()
                            .log("Error occurred  while sending the xml file to Banking")
                    .endChoice()
                .otherwise()
                    .log("XML is not valid, ${header.CamelFileName}")
                    .log("Error message :: ${header.xml_error_messages}")
                    //.throwException(new XmlValidationException("Invalid xml file", SentryLevel.ERROR, "xmlValidationError"))
            .end()
        ;
         
        from("direct:mapPaymentTransactions")
            .log("Start mapping maksuliikenne data")
            .unmarshal(new JacksonDataFormat())
            .bean(mlProcessor, "mapPaymentTransactions")
            .marshal().jacksonXml(Document.class)
            .convertBodyTo(String.class)
            .setBody().groovy("'" + XML_DECLARATION + "'" + " + body")
            .to("mock:mapPaymentTransactions.result")
        ;

        from("direct:out-banking")
            .autoStartup("{{BANKING_SFTP_AUTOSTARTUP}}")
            .log("Sendig the file to banking")
            .to("sftp:{{BANKING_SFTP_HOST_TEST}}:22/{{BANKING_DIRECTORY_PATH}}?username={{BANKING_SFTP_USER_TEST}}"
                + "&password={{BANKING_SFTP_PASSWORD_TEST}}"
                + "&strictHostKeyChecking=no"
            )
            .log("SFTP response :: ${header.CamelFtpReplyCode}  ::  ${header.CamelFtpReplyString}")
        ;

        from("direct:sendMaksuliikenneReportEmail")
            .log("Creating email message")
            .process(ex -> {
                Map<String,Object> totalAmounts = ex.getIn().getHeader("reportData", Map.class);
                String dueDate = ex.getIn().getHeader("dueDate", String.class);
                int amountOfPayments = (int) totalAmounts.get("numberOfPmts");
                BigDecimal totalAmount = (BigDecimal) totalAmounts.get("totalSumOfPmts");
                String message = "Maksupäivä: " + dueDate + "<br>" 
                               + "Maksuja yhteensä: " + amountOfPayments + "<br>"
                               + "Maksujen yhteissumma: " + totalAmount;
                
                String subject = "TESTI: YA-maksut/TYPA";

                ex.getIn().setHeader("messageSubject", subject);
                ex.getIn().setHeader("emailMessage", message);
            })
            //.log("email message subject :: ${header.messageSubject}")
            //.log("email message :: ${header.emailMessage}")
            .bean(sendEmail, "sendEmail")
            .log("Email has been sent")
        ;

    }
}
