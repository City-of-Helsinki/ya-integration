package fi.hel.integration.ya;

import org.apache.camel.builder.RouteBuilder;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProbesRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        
        rest()
            .get("/readiness")
                .to("direct:ready")
	        .get("/healthz")
	            .to("direct:alive")
            
        ;

        from("direct:ready")
            .id("getReady")
            .setBody(constant("Ready"))
        ;
        
        from("direct:alive")
            .id("getAlive")
            .setBody(constant("Alive"))
        ;    
    }
}
