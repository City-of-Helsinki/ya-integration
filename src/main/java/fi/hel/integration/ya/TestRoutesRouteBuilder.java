package fi.hel.integration.ya;

import org.apache.camel.builder.RouteBuilder;

import fi.hel.integration.ya.maksuliikenne.processor.MaksuliikenneProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

// These routes are for testing (e.g.connections)
@ApplicationScoped
public class TestRoutesRouteBuilder extends RouteBuilder {

    @Inject
    MaksuliikenneProcessor mlProcessor;

    @Override
    public void configure() throws Exception {

       // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.

        
        from("timer://testVerkkolevySftp?repeatCount=1")
            .autoStartup("{{VERKKOLEVY_SFTP_TESTROUTE_AUTOSTARTUP}}")
            .log("Starting verkkolevy sftp test route")
            .setHeader("hostname").simple("{{VERKKOLEVY_SFTP_HOST}}")
            .setHeader("username").simple("{{VERKKOLEVY_SFTP_USER}}")
            .setHeader("password").simple("{{VERKKOLEVY_SFTP_PASSWORD}}")
            .setHeader("directoryPath").simple("{{VERKKOLEVY_DIRECTORY_PATH}}")
            .to("direct:fetchDirectoriesFromSftp")
        ;

        from("direct:fetchFileNamesFromSftp")
            .bean(mlProcessor, "getAllSFTPFileNames(*)")
            .log("File names :: ${body}")
        ;

        from("direct:fetchDirectoriesFromSftp")
            .bean(mlProcessor, "getAllSFTPDirectories")
            .log("Directories :: ${body}")
        ;
    }
}
