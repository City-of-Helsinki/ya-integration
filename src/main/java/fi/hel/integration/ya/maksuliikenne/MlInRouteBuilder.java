package fi.hel.integration.ya.maksuliikenne;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.processor.aggregate.GroupedExchangeAggregationStrategy;

import fi.hel.integration.ya.maksuliikenne.processor.MaksuliikenneProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MlInRouteBuilder extends RouteBuilder {

    @Inject
    MaksuliikenneProcessor mlProcessor;

    //private final String testSecret = "{{test_secret}}";
    private final String KIPA_SFTP_HOST = "{{kipa_sftp_host}}";
    private final String KIPA_SFTP_USER_P24 = "{{kipa_sftp_user_p24}}";
    private final String KIPA_SFTP_PASSWORD_P24 = "{{kipa_sftp_password_p24}}";
    private final String KIPA_DIRECTORY_PATH_P24 = "{{KIPA_DIRECTORY_PATH_P24}}";
    
    @Override
    public void configure() throws Exception {

         // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.
   
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
