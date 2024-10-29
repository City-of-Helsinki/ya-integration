package fi.hel.integration.ya;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.RouteBuilder;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import fi.hel.integration.ya.maksuliikenne.processor.MaksuliikenneProcessor;
import io.sentry.Sentry;
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

    public List<String> getAllSFTPFileNames(Exchange ex) throws JSchException, SftpException, IOException {
        String directoryPath = ex.getIn().getHeader("directoryPath", String.class);
        String hostname = ex.getIn().getHeader("hostname", String.class);
        String username = ex.getIn().getHeader("username", String.class);
        String password = ex.getIn().getHeader("password", String.class);

        JSch jsch = new JSch();
        Session session = jsch.getSession(username, hostname, 22);
        session.setPassword( password );
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        List<String> fileNames = new ArrayList<>();
        Vector<ChannelSftp.LsEntry> files = channelSftp.ls(directoryPath);
        for (ChannelSftp.LsEntry file : files) {
            if (!file.getAttrs().isDir()) {
                fileNames.add(file.getFilename());
                System.out.println(file.getFilename());
            }        
        }

        channelSftp.exit();
        session.disconnect();

        return fileNames;
    }
    

    public List<String> getAllSFTPDirectories(Exchange ex) throws JSchException, SftpException, IOException {
        String directoryPath = ex.getIn().getHeader("directoryPath", String.class);
        String hostname = ex.getIn().getHeader("hostname", String.class);
        String username = ex.getIn().getHeader("username", String.class);
        String password = ex.getIn().getHeader("password", String.class);

        // Check for missing or invalid headers
        if (directoryPath == null || hostname == null || username == null || password == null) {
            throw new IllegalArgumentException("Missing one or more required SFTP headers (directoryPath, hostname, username, password).");
        }

        List<String> directoryNames = new ArrayList<>();
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            // Create JSch session and set config
            JSch jsch = new JSch();
            session = jsch.getSession(username, hostname, 22);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();  // Connect to the SFTP server

            // Open SFTP channel
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // List directories in the given path
            Vector<ChannelSftp.LsEntry> files = channelSftp.ls(directoryPath);

            for (ChannelSftp.LsEntry file : files) {
                if (file.getAttrs().isDir()) {
                    directoryNames.add(file.getFilename());
                }
            }
        } catch (JSchException | SftpException e) {
            // Log and wrap the exception
            throw new RuntimeCamelException("SFTP operation failed: " + e.getMessage(), e);
        } finally {
            // Properly close resources
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }

        return directoryNames;
    }

    @Inject
    MaksuliikenneProcessor mlProcessor;

    @Override
    public void configure() throws Exception {

       // Exception handler for route errors. 
        onException(Exception.class) // Catch all the Exception -type exceptions.
            .log("An error occurred: ${exception}") // Log error.
            .process(exchange -> {
                // Get the exception that was caught
                Exception cause = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                
                // Send the exception to Sentry
                if (cause != null) {
                    Sentry.captureException(cause);
                }

                exchange.getIn().setBody("Error sent to Sentry: " + (cause != null ? cause.getMessage() : "Unknown error"));
            })
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

        from("timer://testP24Route?repeatCount=1")
            .autoStartup("{{MAKSULIIKENNE_P24_TESTROUTE_AUTOSTARTUP}}")
            .log("Starting kipa P24 test route")
            .setHeader("hostname").simple("{{KIPA_SFTP_HOST}}")
            .setHeader("username").simple("{{KIPA_SFTP_USER_P24}}")
            .setHeader("password").simple("{{KIPA_SFTP_PASSWORD_P24}}")
            .setHeader("directoryPath").simple("{{KIPA_DIRECTORY_PATH_P24}}")
            .to("direct:fetchFileNamesFromSftp")
        ;

        from("timer://testP23Route?repeatCount=1")
            .autoStartup("{{MAKSULIIKENNE_P23_TESTROUTE_AUTOSTARTUP}}")
            .log("Starting kipa P23 test route")
            .setHeader("hostname").simple("{{KIPA_SFTP_HOST}}")
            .setHeader("username").simple("{{KIPA_SFTP_USER_P23}}")
            .setHeader("password").simple("{{KIPA_SFTP_PASSWORD_P23}}")
            .setHeader("directoryPath").simple("{{KIPA_DIRECTORY_PATH_P23}}")
            .to("direct:fetchFileNamesFromSftp")
            //.to("direct:fetchDirectoriesFromSftp")
        ;


        from("direct:fetchFileNamesFromSftp")
            .bean(this, "getAllSFTPFileNames(*)")
            .log("File names :: ${body}")
        ;

        from("direct:fetchDirectoriesFromSftp")
            .bean(this, "getAllSFTPDirectories")
            .log("Directories :: ${body}")
        ;

        from("sftp:{{KIPA_SFTP_HOST}}:22/{{KIPA_DIRECTORY_PATH_P24}}?username={{KIPA_SFTP_USER_P24}}"
                + "&password={{KIPA_SFTP_PASSWORD_P24}}"
                + "&strictHostKeyChecking=no"
                + "&scheduler=quartz"         
                + "&scheduler.cron={{MAKSULIIKENNE_TEST_TIMER}}" 
                + "&antInclude=YA_p24_091_20241012000003_6_HKK*"
            )   
            .autoStartup("{{MAKSULIIKENNE_TEST_IN_AUTOSTARTUP}}")
            .log("json content :: ${body}")
        ;

        from("timer://testSentry?repeatCount=1&delay=5000")
            .autoStartup("{{SENTRY_TEST_AUTOSTARTUP}}")
            .process(exchange -> {
                throw new Exception("This is a test."); 
            })
        ;

        from("timer://testRedis?repeatCount=1&delay=5000")
            .autoStartup("{{REDIS_TEST_AUTOSTARTUP}}")
            .setBody(constant("redis testi"))
            .setHeader("key", constant("redisTestKey"))
            .bean("redisProcessor", "set(${header.key}, ${body})")
            .log("Value set in Redis with key ${header.key}")
            .bean("redisProcessor", "get(${header.key})")
            .log("Retrieved Redis value: ${body}")
            
        ;
    }     
}

