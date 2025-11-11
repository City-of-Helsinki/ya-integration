package fi.hel.integration.ya.maksuliikenne.Maksupaivapalaute;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MaksupaivapalauteRouteBuilder extends RouteBuilder {

    @Inject
    MaksupaivaProcessor maksupaivaProcessor;
    
    @Override
    public void configure() throws Exception {
    
        // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.


        from("file:inbox/maksupaivapalaute")
            .log("Starting file trigger route for maksupäiväpalaute - file: ${header.CamelFileName}")
            .unmarshal(new JacksonDataFormat())
            .bean(maksupaivaProcessor, "mapPaymentFeedback")
            .marshal(new JacksonDataFormat())
            .setHeader("CamelFileName", simple("maksupaivapalaute_${date:now:yyyyMMdd_HHmmss}.json"))
            .to("file:outbox/maksupaivapalaute")
            .log("Payment feedback file created successfully: ${header.CamelFileName}")
        ;

        // Test route for payment feedback processing
        from("direct:maksupaivapalaute")
            .unmarshal(new JacksonDataFormat())
            .bean(maksupaivaProcessor, "mapPaymentFeedback")
            .marshal(new JacksonDataFormat())
            .to("mock:maksupaivapalaute.result")
        ;
    
    }
}
