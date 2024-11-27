package fi.hel.integration.ya.starttiraha;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.Endpoint;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;

import fi.hel.integration.ya.CsvValidator;
import fi.hel.integration.ya.Utils;
import fi.hel.integration.ya.XmlValidator;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.BenefitReportsRequestToIR;
import fi.hel.integration.ya.starttiraha.processor.TulorekisteriProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TulorekisteriRouteBuilder extends RouteBuilder {

    @Inject
    TulorekisteriProcessor trProcessor;

    @Inject
    Utils utils;

    @Inject 
    XmlValidator xmlValidator;

    @Inject
    CsvValidator csvValidator;

    @EndpointInject("{{app.endpoints.starttiraha.outTulorekisteriXml}}")
    Endpoint outTulorekisteriXml;

    private static final List<String> CSV_HEADERS = Arrays.asList(
        "payerId",
        "paymentDate",
        "hetu",
        "amount",
        "startDate",
        "endDate",
        "taxAmount",
        "garnishmentAmount",
        "paymentDate2",
        "paymentDate3",
        "decisionNumber"
    );

    private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";
    private static final String SCHEMA_FILE = "schema/tulorekisteri/BenefitReportsToIR.xsd";
    private static final int COLUMNS = 11;

    @Override
    public void configure() throws Exception {


        CsvDataFormat csv = new CsvDataFormat();
        csv.setUseMaps(false);
        csv.setDelimiter(';');
        csv.close();
         
        // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.

        from("file:inbox/starttiraha/tulorekisteri?readLock=changed")
            .log("Data ::  ${body}")
            .to("direct:tulorekisteri.controller")
        ;

        from("{{TULOREKISTERI_QUARTZ_TIMER}}")
            .autoStartup("{{TULOREKISTERI_IN_AUTOSTARTUP}}")
            .setHeader("hostname", simple("{{AHR_SFTP_HOST}}"))
            .setHeader("username", simple("{{AHR_SFTP_USER}}"))
            .setHeader("privateKey", simple("{{AHR_SFTP_PRIVATEKEY}}"))
            .setHeader("directoryPath", simple("{{AHR_DIRECTORY_PATH}}"))
            .bean(trProcessor, "fetchFileFromSftp")
            .choice()
                .when(simple("${body} != ''"))
                    .log("Fetched file content: ${body}")
                    .log("Fetched file name: ${header.CamelFileName}")
                    .to("direct:tulorekisteri.controller")
                .otherwise()
                    .log("No files found in the remote directory");

        from("direct:tulorekisteri.controller")
            .log("file name :: ${header.CamelFileName}")
            .setHeader("columns", constant(COLUMNS))
            .bean(csvValidator, "validateCsv(*)")
            .log("IS CSV VALID :: ${header.isCsvValid}")
            .to("direct:create-map")
            .bean(trProcessor, "mapIncomeRegisterData")
            .marshal().jacksonXml(BenefitReportsRequestToIR.class)
            .log("xml body :: ${body}")
            .convertBodyTo(String.class)
            .setBody().groovy("'" + XML_DECLARATION + "'" + " + body")
            .bean(xmlValidator, "validateXml(*," +  SCHEMA_FILE + ")")
            .log("is valid :: ${header.isXmlValid}")
            .setHeader(Exchange.FILE_NAME, simple("${header.CamelFileName.replaceAll('.csv$', '.xml')}"))
            .log("XML BODY :: ${body}")
            //.to(outTulorekisteriXml)
            
        ;

        from("direct:out.tulorekisteri")
            //.to("file:outbox/starttiraha")
            .log("Sending tulorekisteri file to verkkolevy sftp")
            //.to("sftp:{{VERKKOLEVY_SFTP_HOST}}:22/ture?username={{VERKKOLEVY_SFTP_USER}}&password={{VERKKOLEVY_SFTP_PASSWORD}}&throwExceptionOnConnectFailed=true&strictHostKeyChecking=no")
            //.log("SFTP response :: ${header.CamelFtpReplyCode}  ::  ${header.CamelFtpReplyString}")   
        ;

        from("direct:create-map")
            .unmarshal(csv)
            .process(exchange -> {
            // Get the body as a list of lists (rows and columns)
                List<List<String>> csvData = exchange.getIn().getBody(List.class);

                //System.out.println("CSV data ::" + csvData);

                // Map the CSV data to a list of maps using the custom headers
                List<Map<String, String>> mappedData = csvData.stream()
                    .map(row -> {
                        Map<String, String> map = new LinkedHashMap<>();
                        for (int i = 0; i < CSV_HEADERS.size(); i++) {
                            map.put(CSV_HEADERS.get(i), row.size() > i ? row.get(i) : null);
                    }
                        return map;
                    })
                        
                    .collect(Collectors.toList());
                    //System.out.println("Mapped data :: " + mappedData);
            
                    // Set the mapped data as the body if further processing is needed
                    exchange.getIn().setBody(mappedData);
                    System.out.println("MAPPED data :: " + mappedData.getClass().getName());
                })
            .log("body after adding headers :: ${body}")
            .to("mock:create-map.result")
        ;
    }
}
