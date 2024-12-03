package fi.hel.integration.ya.maksuliikenne;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.file.remote.SftpComponent;
import org.apache.camel.component.file.remote.SftpConfiguration;
import org.apache.camel.component.jackson.JacksonDataFormat;

import com.jcraft.jsch.JSch;

import fi.hel.integration.ya.XmlValidator;
import fi.hel.integration.ya.maksuliikenne.models.kirjanpitoSAP.SBO_SimpleAccountingContainer;
import fi.hel.integration.ya.maksuliikenne.processor.KirjanpitoProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KirjanpitoRouteBuilder extends RouteBuilder {

    @Inject
    KirjanpitoProcessor kpProcessor;

    @Inject
    XmlValidator xmlValidator;

    
    
    private final String SCHEMA_FILE = "schema/sap/SBO_SimpleAccountingContainer.xsd";
    private final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private final String FILE_NAME_PREFIX= "{{MAKSULIIKENNE_KIRJANPITO_FILENAMEPREFIX}}";
    private final String SENDER_ID = "{{MAKSULIIKENNE_KIRJANPITO_SENDERID}}";

    @Override
    public void configure() throws Exception {

        // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.
    
        // This route is for local testing
        from("file:inbox/maksuliikenne/kirjanpito?readLock=changed")
            //.log("Data ::  ${body}")
            .to("direct:kirjanpito.controller")
        ;

        from("direct:kirjanpito.controller")
            //.log("BODY :: ${body}")
            .log("Preparing to handle accounting data")
            .unmarshal(new JacksonDataFormat())
            .split(body())
                //.log("Splitted body :: ${body}")
                .to("direct:mapAccountingData")
                .bean(xmlValidator, "validateXml(*," +  SCHEMA_FILE + ")")
                .setVariable("claimTypeCode")
                    .language("groovy", "def filename = request.headers.jsonFileName; filename.split('_')[-1].replace('.json', '')")
                .setHeader(Exchange.FILE_NAME, simple(FILE_NAME_PREFIX + SENDER_ID + "_${variable.claimTypeCode}_${date:now:yyyyMMddHHmmssSSS}.xml"))
                .choice()
                    .when().simple("${header.isXmlValid} == 'true'")
                        .log("is valid :: ${header.isXmlValid}")
                        //.to("file:outbox/maksuliikenne/sap")
                        .log("Created kirjanpito xml, file name :: ${header.CamelFileName}")
                        .to("direct:out.maksuliikenne-sap")
                        //.log("Kirjanpito xml :: ${body}")
                    .otherwise()
                        //.to("file:outbox/invalidXml")
                        .log("XML is not valid, errors :: ${header.xml_error_messages}, lines :: ${header.xml_error_line_numbers}, columns :: ${header.xml_error_column_numbers}")
                .end() 
            .end()
            .log("All accounting data processed")
            .to("direct:sendMaksuliikenneReportEmail")
            
                
        ;

        from("direct:mapAccountingData")
            .bean(kpProcessor, "mapAccountigData(*)")
            .marshal().jacksonXml(SBO_SimpleAccountingContainer.class)
            .convertBodyTo(String.class)
            .setBody().groovy("'" + XML_DECLARATION + "'" + " + body")
            .to("mock:mapAccountingData.result")
        ;

        from("direct:out.maksuliikenne-sap")
            .log("Sending file to sap")
            .setHeader("hostname").simple("{{SAP_SFTP_HOST}}")
            .setHeader("username").simple("{{SAP_SFTP_USER}}")
            .setHeader("password").simple("{{SAP_SFTP_PASSWORD}}")
            .setHeader("directoryPath").simple("{{SAP_DIRECTORY_PATH}}")
            .bean(kpProcessor, "writeFileSapSftp")
            .log("SFTP response :: ${header.CamelFtpReplyCode}  ::  ${header.CamelFtpReplyString}")
        ;
    }
}
