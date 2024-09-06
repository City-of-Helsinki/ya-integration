package fi.hel.integration.ya.maksuliikenne;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MlInRouteBuilder extends RouteBuilder {

    private final String testSecret = "{{test-secret}}";
    private final String fileNamePrefix = "{{MAKSULIIKENNE_BANKING_FILENAMEPREFIX}}";
    
    @Override
    public void configure() throws Exception {

         // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.

        from("timer://testRoute?repeatCount=1")
            .autoStartup(true)
            .log("Starting test route")
            .log("file name prefix  :: " + fileNamePrefix)
            .log("test secret :: " + testSecret)
        ;
        
        from("file:inbox/maksuliikenne?readLock=changed")
            .unmarshal(new JacksonDataFormat())
            .aggregate(new GroupedExchangeAggregationStrategy()).constant(true)
                .completionSize(1000) 
                .completionTimeout(5000)
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
            //.to("file:outbox/kipaResult")
            .to("direct:ml-controller")
            
        ;
    }
}
