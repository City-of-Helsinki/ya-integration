package fi.hel.integration.ya;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import fi.hel.integration.ya.maksuliikenne.processor.MaksuliikenneProcessor;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

// These routes are for testing (e.g.connections)
@ApplicationScoped
public class TestRoutesRouteBuilder extends RouteBuilder {

    public boolean testSFTPConnection(Exchange exchange) {
        // Extract SFTP connection details from Exchange headers
        String hostname = exchange.getIn().getHeader("hostname", String.class);
        String username = exchange.getIn().getHeader("username", String.class);
        String password = exchange.getIn().getHeader("password", String.class);
        int port = exchange.getIn().getHeader("port", 22, Integer.class);  // Default to 22 if not provided

        // Check if mandatory headers are present
        if (hostname == null || username == null || password == null) {
            throw new IllegalArgumentException("Missing SFTP connection details (hostname, username, or password).");
        }

        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            // Setup JSch session
            JSch jsch = new JSch();
            session = jsch.getSession(username, hostname, port);  // Use port from headers, default to 22
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");   // Disable host key checking for testing
            session.connect();  // Connect to the SFTP server

            // Open the SFTP channel
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // Connection is successful
            System.out.println("SFTP connection successful");
            return true;
        } catch (JSchException e) {
            // Log the error and return false if the connection failed
            System.err.println("SFTP connection failed: " + e.getMessage());
            return false;
        
        } finally {
            // Ensure resources are properly closed
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    @Inject
    MaksuliikenneProcessor mlProcessor;

    @Override
    public void configure() throws Exception {

       // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .handled(true) // The error is not passed on to other error handlers.
            .stop(); // Stop routing processing for this error.

        
        from("timer://testVerkkolevySftp?repeatCount=1&delay=5000")
            .autoStartup("{{VERKKOLEVY_SFTP_TESTROUTE_AUTOSTARTUP}}")
            .log("Starting verkkolevy sftp test route")
            .setHeader("hostname").simple("{{VERKKOLEVY_SFTP_HOST}}")
            .setHeader("username").simple("{{VERKKOLEVY_SFTP_USER}}")
            .setHeader("password").simple("{{VERKKOLEVY_SFTP_PASSWORD}}")
            //.setHeader("directoryPath").simple("{{VERKKOLEVY_DIRECTORY_PATH}}")
            .bean(this, "testSFTPConnection")
        ;

        from("direct:fetchFileNamesFromSftp")
            .bean(mlProcessor, "getAllSFTPFileNames(*)")
            .log("File names :: ${body}")
        ;

        from("direct:fetchDirectoriesFromSftp")
            .bean(mlProcessor, "getAllSFTPDirectories")
            .log("Directories :: ${body}")
        ;

        from("sftp:{{KIPA_SFTP_HOST}}:22/{{KIPA_DIRECTORY_PATH_P24}}?username={{KIPA_SFTP_USER_P24}}"
                + "&password={{KIPA_SFTP_PASSWORD_P24}}"
                + "&strictHostKeyChecking=no"
                + "&scheduler=quartz"         
                + "&scheduler.cron={{MAKSULIIKENNE_TEST_TIMER}}" 
                + "&antInclude=YA_p22_091_20240930153015_091_SR*"
            )   
            .autoStartup("{{MAKSULIIKENNE_TEST_IN_AUTOSTARTUP}}")
            .log("json content :: ${body}")
        ;
    }
}
