package fi.hel.integration.ya.kipa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;

import fi.hel.integration.ya.JsonValidator;
import fi.hel.integration.ya.maksuliikenne.processor.MaksuliikenneProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KipaInRouteBuilder extends RouteBuilder{

    @Inject
    JsonValidator jsonValidator;

    @Inject
    MaksuliikenneProcessor mlProcessor;

    private final String SCHEMA_FILE_PT_PT55_TOJT = "schema/kipa/json_schema_PT_PT55_TOJT.json";
    private final String SCHEMA_FILE_MYK_HKK = "schema/kipa/json_schema_MYK_HKK.json";
    private final String SCHEMA_FILE_SR = "schema/kipa/json_schema_SR.json";


    @Override
    public void configure() throws Exception {

       // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.

        // This route is for local development and testing
        // The route is triggered by dropping json file/files into folder inbox/kipa/P22
        from("file:inbox/kipa/P22")
            .log("body :: ${body}")
            .to("direct:validate-json")
            .choice()
                .when(simple("${header.isJsonValid} == 'true'"))
                    .log("Json is valid continue processing ${header.CamelFileName}")
                    .to("direct:continue-processing-P22Data")
                .otherwise()
                    .log("Json is not valid, ${header.CamelFileName}")
                    .to("file:outbox/invalidJson")
        ;

        from("direct:continue-processing-P22Data")
            .unmarshal(new JacksonDataFormat())
            .aggregate(new GroupedExchangeAggregationStrategy()).constant(true)
                .completionSize(1000) 
                .completionTimeout(10000)
                .process(exchange -> {
                    //System.out.println("BODY :: " + exchange.getIn().getBody());
                    List<Exchange> combinedExchanges = exchange.getIn().getBody(List.class);
                    List<Map<String, Object>> combinedJsons = new ArrayList<>();
                    for (Exchange ex : combinedExchanges) {
                        Map<String, Object> json = ex.getIn().getBody(Map.class);
                        combinedJsons.add(json);
                    }
                    
                    exchange.getIn().setBody(combinedJsons);
                })
            
            .marshal(new JacksonDataFormat())
            //.to("file:outbox/test")
            .log("Combined jsons :: ${body}")
            .to("direct:sr-controller")
        ;

        /* from("direct:readSFTPFileAndMove")
            .pollEnrich()
                .simple("sftp:{{KIPA_SFTP_HOST}}:22/{{KIPA_DIRECTORY_PATH_P24}}?username={{KIPA_SFTP_USER_P24}}&password={{KIPA_SFTP_PASSWORD_P24}}&strictHostKeyChecking=no&fileName=${headers.CamelFileName}&move=../${variable.kipa_dir}")
                .timeout(10000)
            .log("CamelFtpReplyString: ${headers.CamelFtpReplyString}")
        ; */
    }
}
