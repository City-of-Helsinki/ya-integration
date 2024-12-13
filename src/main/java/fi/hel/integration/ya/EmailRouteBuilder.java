package fi.hel.integration.ya;

import org.apache.camel.builder.RouteBuilder;

import fi.hel.integration.ya.maksuliikenne.processor.SendEmail;
import jakarta.inject.Inject;

public class EmailRouteBuilder extends RouteBuilder {
    
    @Inject
    SendEmail sendEmail;
    
    @Override
    public void configure() throws Exception {

        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.

        
        from("direct:sendErrorReport")
            .log("Creating error message via email")
            .process(ex -> {
                String fileName = ex.getIn().getHeader("CamelFileName", String.class);
                String errorMessage = ex.getIn().getHeader("error_messages", String.class);
                String message = "Virheellinen tiedosto: " + fileName + "<br>" 
                               + "Virhe: Invalid file. " + errorMessage + "<br>"
                               + "Tämä on YA-integraation lähettämä automaattinen viesti";
                
                ex.getIn().setHeader("emailMessage", message);
            })
            .log("Preparing to send email")
            .bean(sendEmail, "sendEmail")
            .log("Email has been sent")
        ;
    }
}
