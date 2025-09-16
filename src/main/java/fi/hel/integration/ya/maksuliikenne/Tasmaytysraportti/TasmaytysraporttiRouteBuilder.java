package fi.hel.integration.ya.maksuliikenne.Tasmaytysraportti;

import org.apache.camel.builder.RouteBuilder;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TasmaytysraporttiRouteBuilder extends RouteBuilder {
    
    @Inject
    TasmaytysraporttiCombineProcessor tasmaytysraporttiCombineProcessor;

    @Override
    public void configure() throws Exception {
        
        onException(Exception.class)
            .log("Error processing täsmäytysraportti: ${exception.message}")
            .handled(true)
            .to("file:outbox/errors?fileName=tasmaytysraportti-error-${date:now:yyyyMMdd-HHmmss}.txt");

        // Timer route that triggers once at startup
        from("timer:tasmaytysraportti-starter?repeatCount=1")
            .routeId("tasmaytysraportti-starter")
            .autoStartup("{{TASMAYTYSRAPORTTI_AUTOSTARTUP}}")
            .log("Starting täsmäytysraportti processing at application startup")
            .to("direct:process-tasmaytysraportti");

        // Main processing route that reads all JSON files and creates combined CSV
        from("direct:process-tasmaytysraportti")
            .routeId("process-tasmaytysraportti")
            .log("Processing all täsmäytysraportti JSON files from kipa")
            .setHeader("hostname").simple("{{KIPA_SFTP_HOST}}")
            .setHeader("username").simple("{{KIPA_SFTP_USER_P24}}")
            .setHeader("password").simple("{{KIPA_SFTP_PASSWORD_P24}}")
            .setHeader("directoryPath").simple("{{KIPA_DIRECTORY_PATH_P24}}")
            .setHeader("kipa_container", simple("P24"))
            .setHeader("filePrefix", constant("YATE_tasmaytysraportti_p24"))
            .process(tasmaytysraporttiCombineProcessor)
            .choice()
                .when(simple("${body} != null && ${body} != ''"))
                    .process(exchange -> {
                        String body = exchange.getIn().getBody(String.class);
                        // Prepend a UTF-8 Byte Order Mark (BOM) to the body.
                        // This ensures that programs like Excel correctly interpret the file as UTF-8 encoded,
                        // avoiding issues with special characters (e.g., ä, ö, ü) being displayed incorrectly.
                        exchange.getIn().setBody("\uFEFF" + body);
                    })
                    //.to("file:outbox/täsmäytysraportit?fileName=${header.CamelFileName}")
                    .log("Combined CSV saved: ${header.CamelFileName}")
                    .log("CSV :: ${body}")
                .otherwise()
                    .log("No data to process - no JSON files found or no records with allowed claim types")
            .endChoice();
    }
}