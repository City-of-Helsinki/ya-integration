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

        from("direct:ml-controller")
            //.to("file:outbox/test")
            .to("direct:mapPaymentTransactions")
            .bean(xmlValidator, "validateXml(*," +  SCHEMA_FILE + ")")
            .log("xml is valid :: ${header.isXmlValid}")
            .setHeader(Exchange.FILE_NAME, simple(FILE_NAME_PREFIX + "${date:now:yyyyMMddHHmmss}.xml"))
            .to("mock:sendMaksuliikenneXml")
            .to("file:outbox/maksuliikenne")
            .log("Pain xml :: ${body}")
            .choice()
                .when(simple("${header.isXmlValid} == 'true'"))
                .log("XML is valid, sending the file to banking ${header.CamelFileName}")
                .to("direct:out-banking")
            .otherwise()
                .log("XML is not valid, ${header.CamelFileName}")
    
        ;
        

        from("direct:mapPaymentTransactions")
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
    }


}
