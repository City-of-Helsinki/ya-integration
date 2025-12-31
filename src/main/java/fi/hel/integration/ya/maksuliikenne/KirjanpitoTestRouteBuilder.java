package fi.hel.integration.ya.maksuliikenne;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;

import fi.hel.integration.ya.JsonValidator;
import fi.hel.integration.ya.RedisProcessor;
import fi.hel.integration.ya.SendEmail;
import fi.hel.integration.ya.SftpProcessor;
import fi.hel.integration.ya.ValidateJsonProcessor;
import fi.hel.integration.ya.XmlValidator;
import fi.hel.integration.ya.exceptions.XmlValidationException;
import fi.hel.integration.ya.maksuliikenne.processor.MaksuliikenneProcessor;
import io.sentry.SentryLevel;
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class KirjanpitoTestRouteBuilder extends RouteBuilder {

    @Inject
    JsonValidator jsonValidator;

    @Inject
    RedisProcessor redisProcessor;

    @Inject
    SftpProcessor sftpProcessor;

    @Inject 
    ValidateJsonProcessor validateJsonProcessor;

    @Inject
    XmlValidator xmlValidator;

   
    private final String SCHEMA_FILE = "schema/sap/SBO_SimpleAccountingContainer.xsd";
    private final String FILE_NAME_PREFIX= "{{MAKSULIIKENNE_KIRJANPITO_FILENAMEPREFIX}}";
    private final String SENDER_ID = "{{MAKSULIIKENNE_KIRJANPITO_SENDERID}}";


    private final String LOCK_KEY = "timer-route-lock";

    @Override
    public void configure() throws Exception {

         // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.
        
        // Simplified version - only kirjanpito processing
        from("{{KIRJANPITO_TEST_QUARTZ_TIMER}}")
            .routeId("kirjanpito-test-route")
            .autoStartup("{{KIRJANPITO_TEST_AUTOSTARTUP}}")
            .log("Kirjanpito test route started")
            .process(exchange -> {
                if (redisProcessor.acquireLock(LOCK_KEY, 300)) { 
                    exchange.getIn().setHeader("lockAcquired", true);
                    System.out.println("Lock acquired, processing starts");

                } else {
                    exchange.getIn().setHeader("lockAcquired", false);
                    System.out.println("Lock not acquired, skipping processing");
                }
            })
            .filter(header("lockAcquired").isEqualTo(true))
                .log("Start route to fetch files from kipa P24")
                .setHeader("hostname").simple("{{VERKKOLEVY_SFTP_HOST}}")
                .setHeader("username").simple("{{VERKKOLEVY_SFTP_USER}}")
                .setHeader("password").simple("{{VERKKOLEVY_SFTP_PASSWORD}}")
                .setHeader("directoryPath").simple("{{VERKKOLEVY_DIRECTORY_PATH}}")
                .setHeader("kipa_container", simple("P24"))
                .setHeader("filePrefix", constant("YA_p24_091_20251223"))
                //.setHeader("filePrefix2", constant("YA_p24_091_20241216155712_091_PT55.json"))
                .log("Fetching file names from Kipa")
                .bean(sftpProcessor, "getAllSFTPFileNames")
                .log("Files to be processed(before filtering) :: ${body}")
                .to("direct:filter-ya-p24-files")
                .log("Files to be processed (after filtering) :: ${body}")
                .choice()
                    .when(simple("${body} == null || ${body.size()} == 0"))
                        .log("No files found in SFTP.")
                    .otherwise()
                        .log("Files found. Continuing processing.")
                        .log("Fetching and combining the json data")
                        .bean(sftpProcessor, "fetchAllFilesFromSftp")
                        //.log("Body after fetching files :: ${body}")
                        .bean(validateJsonProcessor, "validateFiles")
                        .setBody().variable("validFiles")
                        .marshal(new JacksonDataFormat())
                        .setVariable("kirjanpito_data").simple("${body}")
                        .to("direct:kirjanpito-test-controller")               
                .end()
            .end()
        ;

        from("direct:kirjanpito-test-controller")
            .log("Preparing to handle accounting data")
            .unmarshal(new JacksonDataFormat())
            // .process(exchange -> {
            //     // Muuta kaikkien JSON-objektien businessId
            //     java.util.List<Map<String, Object>> jsonList = exchange.getIn().getBody(java.util.List.class);
            //     for (Map<String, Object> jsonObject : jsonList) {
            //         if (jsonObject.containsKey("receiver")) {
            //             @SuppressWarnings("unchecked")
            //             Map<String, Object> receiver = (Map<String, Object>) jsonObject.get("receiver");
            //             receiver.put("businessId", "0201256-6");
            //         }
            //     }
            //     exchange.getIn().setBody(jsonList);
            // })
            .split(body())
                //.log("Splitted body :: ${body}")
                .setVariable("businessId")
                    .language("groovy", "def businessId = request.body.receiver.businessId; businessId")
                .log("Business id :: ${variable.businessId}")
                .choice()
                    .when().simple("${variable.businessId} == '{{MAKSULIIKENNE_KIRJANPITO_HKI_BUSINESSID}}'")
                        .log("Routing to mapAccountingDataSotepe for businessId: ${variable.businessId}")
                        .to("direct:mapAccountingDataSotepe")
                    .otherwise()
                        .log("Routing to mapAccountingData for businessId: ${variable.businessId}")
                        .to("direct:mapAccountingData")
                .end()
                .bean(xmlValidator, "validateXml(*," +  SCHEMA_FILE + ")")
                .setVariable("claimTypeCode")
                    .language("groovy", "def filename = request.headers.jsonFileName; filename.split('_')[-1].replace('.json', '')")
                .setHeader(Exchange.FILE_NAME, simple(FILE_NAME_PREFIX + SENDER_ID + "_${variable.claimTypeCode}_${date:now:yyyyMMddHHmmssSSS}.xml"))
                .choice()
                    .when().simple("${header.isXmlValid} == 'true'")
                        .log("is valid :: ${header.isXmlValid}")
                        .log("kipa json file :: ${header.jsonFileName}")
                        .log("Created kirjanpito xml, file name :: ${header.CamelFileName}")
                        .log("kirjanpito xml :: ${body}")
                        //.to("file:outbox/maksuliikenne/sap")
                        //.to("direct:out.kirjanpito-test-sap")
                    .otherwise()
                        .log("XML is not valid, ${header.CamelFileName}")
                .end() 
            .end()
            .log("All accounting data processed")
        ; 
            
        // from("direct:out.kirjanpito-test-sap")
        //     .log("Sending file to sap, kipa file :: ${header.jsonFileName}")
        //     .to("sftp:{{SAP_SFTP_HOST}}:22/?username={{SAP_SFTP_USER}}"
        //         + "&password={{SAP_SFTP_PASSWORD}}"
        //         + "&strictHostKeyChecking=no"
        //         + "&serverHostKeys=ssh-ed25519,rsa-sha2-256,rsa-sha2-512,ecdsa-sha2-nistp521"
        //         + "&keyExchangeProtocols=curve25519-sha256,curve25519-sha256@libssh.org,ecdh-sha2-nistp384,ecdh-sha2-nistp256,diffie-hellman-group14-sha256,diffie-hellman-group16-sha512,diffie-hellman-group-exchange-sha256"
        //         + "&maximumReconnectAttempts=5" 
        //         + "&reconnectDelay=5000"                 
        //     )
        //     .log("SFTP response :: ${header.CamelFtpReplyCode}  ::  ${header.CamelFtpReplyString}")
        // ;
    }   
}
