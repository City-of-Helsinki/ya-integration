package fi.hel.integration.ya;

import org.apache.camel.builder.RouteBuilder;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ControllerRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {

         // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.

        
        from("timer://smoketest.route?repeatCount=1")
            .log("Smoketest OK")
        ;
    }
    
}
