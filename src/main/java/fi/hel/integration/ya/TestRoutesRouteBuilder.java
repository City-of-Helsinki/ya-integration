package fi.hel.integration.ya;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Vector;

import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import fi.hel.integration.ya.maksuliikenne.processor.MaksuliikenneProcessor;
import fi.hel.integration.ya.starttiraha.processor.TulorekisteriProcessor;
import io.sentry.Sentry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import jakarta.mail.util.ByteArrayDataSource;
import jakarta.activation.*;
import java.util.Properties;
import java.util.Random;

// These routes are for testing (e.g.connections)
@ApplicationScoped
public class TestRoutesRouteBuilder extends RouteBuilder {

    @Inject
    TulorekisteriProcessor tProcessor; 

    @Inject
    RedisProcessor redisProcessor;
    
    @Inject
    SftpProcessor sftpProcessor;

    public boolean testSFTPConnection(Exchange exchange) {
        // Extract SFTP connection details from Exchange headers
        String hostname = exchange.getIn().getHeader("hostname", String.class);
        String username = exchange.getIn().getHeader("username", String.class);
        String password = exchange.getIn().getHeader("password", String.class);
        int port = exchange.getIn().getHeader("port", 22, Integer.class);  // Default to 22 if not provided
        java.util.Properties sftpConfig = exchange.getIn().getHeader("sftp_config", java.util.Properties.class);
       
        if (hostname == null || username == null || password == null) {
            throw new IllegalArgumentException("Missing SFTP connection details (hostname, username, or password).");
        }

        try (Socket socket = new Socket(hostname, port)) {
            System.out.println("Successfully connected to " + hostname + ":" + port + " (pre-SFTP check)");
        } catch (IOException e) {
            System.err.println("Unable to reach " + hostname + ":" + port + ". Check network/firewall settings.");
            return false;
        }

        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            // Setup JSch session
            JSch jsch = new JSch();
            session = jsch.getSession(username, hostname, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");   
            
            if (sftpConfig != null && !sftpConfig.isEmpty()) {
                session.setConfig(sftpConfig); 
            }
            
            session.connect();

            if (session.isConnected()) {
                System.out.println("Session connected successfully to " + hostname + ":" + port);
            } else {
                System.err.println("Session failed to connect to " + hostname + ":" + port);
                return false;
            }

            // Open the SFTP channel
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            if (channelSftp.isConnected()) {
                System.out.println("SFTP channel opened and connected successfully.");
            } else {
                System.err.println("Failed to open SFTP channel.");
                return false;
            }

            // Connection is successful
            System.out.println("SFTP connection successful");
            return true;

        } catch (JSchException e) {
            // Log the error and return false if the connection failed
            System.err.println("SFTP connection failed: " + e.getMessage());
            System.err.println("Detailed cause: " + e.getCause());

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

    public boolean testSFTPConnectionWithPrivateKey(Exchange exchange) {
        // Extract SFTP connection details from Exchange headers
        String hostname = exchange.getIn().getHeader("hostname", String.class);
        String username = exchange.getIn().getHeader("username", String.class);
        String privateKeyEncoded = exchange.getIn().getHeader("privateKey", String.class);
        String privateKey = new String(Base64.getDecoder().decode(privateKeyEncoded));
        int port = exchange.getIn().getHeader("port", 22, Integer.class);  // Default to 22 if not provided
    
        System.out.println("hostname :: " + hostname);
        // Check if mandatory headers are present
        if (hostname == null || username == null || privateKey == null) {
            throw new IllegalArgumentException("Missing SFTP connection details (hostname, username, or privateKey).");
        }

        try {
            InetAddress address = InetAddress.getByName(hostname);
            System.out.println("Hostname resolved to IP: " + address.getHostAddress());
       
        } catch (UnknownHostException e) {
            System.err.println("Failed to resolve hostname: " + hostname + ". Verify DNS settings or hostname.");
            return false;
        }

        try (Socket socket = new Socket(hostname, port)) {
            System.out.println("Successfully connected to " + hostname + ":" + port + " (pre-SFTP check)");
        } catch (IOException e) {
            System.err.println("Unable to reach " + hostname + ":" + port + ". Check network/firewall settings.");
            return false;
        }
    
        Session session = null;
        ChannelSftp channelSftp = null;
    
        try {
            // Setup JSch session with private key
            JSch jsch = new JSch();
            jsch.addIdentity("sftp-identity", privateKey.getBytes(), null, null);
            session = jsch.getSession(username, hostname, port);  
            session.setConfig("StrictHostKeyChecking", "no");  
            session.connect(); 
    
            if (session.isConnected()) {
                System.out.println("Session connected successfully to " + hostname + ":" + port);
            } else {
                System.err.println("Session failed to connect to " + hostname + ":" + port);
                return false;
            }
    
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
    
            if (channelSftp.isConnected()) {
                System.out.println("SFTP channel opened and connected successfully.");
            } else {
                System.err.println("Failed to open SFTP channel.");
                return false;
            }
    
            // Connection is successful
            System.out.println("SFTP connection successful");
            return true;
    
        } catch (JSchException e) {
            // Log the error and return false if the connection failed
            System.err.println("SFTP connection failed: " + e.getMessage());
            System.err.println("Detailed cause: " + e.getCause());
    
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

    public List<String> getAllSFTPDirectories(Exchange ex) throws JSchException, SftpException, IOException {
        String directoryPath = ex.getIn().getHeader("directoryPath", String.class);
        String hostname = ex.getIn().getHeader("hostname", String.class);
        String username = ex.getIn().getHeader("username", String.class);
        String password = ex.getIn().getHeader("password", String.class);
        String privateKeyEncoded = ex.getIn().getHeader("privateKey", String.class);
        String privateKey = null;
        
        if(privateKeyEncoded != null) {
           privateKey = new String(Base64.getDecoder().decode(privateKeyEncoded));
        }

        java.util.Properties sftpConfig = ex.getIn().getHeader("sftp_config", java.util.Properties.class);

        // Check for missing or invalid headers
        if (directoryPath == null || hostname == null || username == null || (password == null && privateKey == null)) {
            throw new IllegalArgumentException("Missing one or more required SFTP headers (directoryPath, hostname, username, and either password or privateKey.");
        }

        List<String> directoryNames = new ArrayList<>();
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            // Create JSch session and set config
            JSch jsch = new JSch();

            if (privateKey != null) {
                jsch.addIdentity("sftp-identity", privateKey.getBytes(), null, null);
            }
            
            session = jsch.getSession(username, hostname, 22);

            if (password != null) {
                session.setPassword(password);
            }

            if (sftpConfig != null && !sftpConfig.isEmpty()) {
                session.setConfig(sftpConfig); 
            }
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

    @ConfigProperty(name="TEST_EMAIL_RECIPIENTS", defaultValue = "recipient")
    String recipients;

    @ConfigProperty(name="MAIL_SMTP_HOST", defaultValue = "host")
    String host;

    @ConfigProperty(name="MAIL_SMTP_PORT", defaultValue = "port")
    String port;

    @ConfigProperty(name="MAIL_SMTP_SENDER", defaultValue = "sender")
    String sender;

    public void sendJsonFileByEmail(Exchange ex) {
        try {
            // Retrieve the email parameters
            String messageSubject = (String) ex.getIn().getHeader("messageSubject");
            String emailMessage = (String) ex.getIn().getHeader("emailMessage");
            String filename = (String) ex.getIn().getHeader("CamelFileName"); 
            byte[] fileContent = ex.getIn().getBody(byte[].class);
            
            System.out.println("Sending email to " + recipients);
    
            Properties prop = new Properties();
            prop.put("mail.smtp.starttls.enable", "true");
            prop.put("mail.smtp.host", host);
            prop.put("mail.smtp.port", port);
    
            jakarta.mail.Session session = jakarta.mail.Session.getInstance(prop);
    
            // Create the email message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
            message.setSubject(messageSubject);
    
            // Create a multipart message for attachment
            Multipart multipart = new MimeMultipart();
    
            // Add the email message as the text part
            MimeBodyPart textBodyPart = new MimeBodyPart();
            textBodyPart.setText(emailMessage, "utf-8", "html");
            multipart.addBodyPart(textBodyPart);
    
            // Add the attachment
            MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            DataSource source = new ByteArrayDataSource(fileContent, "application/json"); // Set MIME type to JSON
            attachmentBodyPart.setDataHandler(new DataHandler(source));
            attachmentBodyPart.setFileName(filename); // Set the attachment filename
            multipart.addBodyPart(attachmentBodyPart);
    
            // Set the content of the email
            message.setContent(multipart);
    
            // Send the email
            Transport.send(message);
            System.out.println("Email sent successfully with JSON attachment.");
    
        } catch (Exception e) {
            log.error("An error occurred while sending email: ", e);
            e.printStackTrace();
            ex.setException(e);
        }
    }

    private static final String LOCK_KEY = "timer-route-lock"; // Redis key for the lock

    /* private boolean acquireLock() {
        try {
            System.out.println("Attempting to acquire lock");
            boolean isLocked = redisProcessor.set(LOCK_KEY, "locked", 120); // 60-second TTL
            System.out.println("Lock acquisition result: " + isLocked);
            if (isLocked) {
                System.out.println("Lock acquired by pod");
                return true;
            } else {
                System.out.println("Lock not acquired by pod");
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to acquire lock from Redis", e);
        }
    } */

    private void releaseLock() {
        try {
            redisProcessor.delete(LOCK_KEY);
        } catch (Exception e) {
            throw new RuntimeException("Failed to release lock from Redis", e);
        }
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
            .setHeader("directoryPath").simple("{{VERKKOLEVY_DIRECTORY_PATH}}")
            .bean(this, "testSFTPConnection")
            //.to("direct:fetchDirectoriesFromSftp")
            //.to("direct:fetchFileNamesFromSftp")    
        ;

        from("timer://testAHRSftp?repeatCount=1&delay=5000")
            .autoStartup("{{AHR_SFTP_TESTROUTE_AUTOSTARTUP}}")
            .log("Starting AHR sftp test route")
            .setHeader("hostname").simple("{{AHR_SFTP_HOST}}")
            .setHeader("username").simple("{{AHR_SFTP_USER}}")
            .setHeader("privateKey").simple("{{AHR_SFTP_PRIVATEKEY}}")
            .setHeader("directoryPath").simple("{{AHR_DIRECTORY_PATH}}")
            .bean(this, "testSFTPConnectionWithPrivateKey")
            //.to("direct:fetchDirectoriesFromSftp")
            //.to("direct:fetchFileNamesFromSftp")
            //.bean(tProcessor, "removeFileFromSftp(*)")            
        ;

        from("timer://testSapSftp?repeatCount=1&delay=5000")
            .autoStartup("{{SAP_SFTP_TESTROUTE_AUTOSTARTUP}}")
            .log("Starting SAP sftp test route")
            .setHeader("hostname").simple("{{SAP_SFTP_HOST}}")
            .setHeader("username").simple("{{SAP_SFTP_USER}}")
            .setHeader("password").simple("{{SAP_SFTP_PASSWORD}}")
            .setHeader("directoryPath").simple("{{SAP_DIRECTORY_PATH}}")
            .process(ex -> {
                // Configure algorithms for compatibility with the server
                java.util.Properties config = new java.util.Properties();
                config.put("kex", "diffie-hellman-group1-sha1,diffie-hellman-group14-sha1");
                config.put("server_host_key", "ssh-rsa");
                ex.getIn().setHeader("sftp_config", config);
            })
            .bean(this, "testSFTPConnection")
            //.to("direct:fetchDirectoriesFromSftp")            
        ;

        from("timer://testP24Route?repeatCount=1")
            .autoStartup("{{MAKSULIIKENNE_P24_TESTROUTE_AUTOSTARTUP}}")
            .log("Starting kipa P24 test route")
            .setHeader("hostname").simple("{{KIPA_SFTP_HOST}}")
            .setHeader("username").simple("{{KIPA_SFTP_USER_P24}}")
            .setHeader("password").simple("{{KIPA_SFTP_PASSWORD_P24}}")
            .setHeader("directoryPath").simple("{{KIPA_DIRECTORY_PATH_P24}}")
            .to("direct:fetchFileNamesFromSftp")
            .to("direct:fetchDirectoriesFromSftp")
        ;

        from("timer://testP22Route?repeatCount=1")
            .autoStartup("{{STARTTIRAHA_P22_TESTROUTE_AUTOSTARTUP}}")
            .log("Starting kipa P22 test route")
            .setHeader("hostname").simple("{{KIPA_SFTP_HOST}}")
            .setHeader("username").simple("{{KIPA_SFTP_USER_P22}}")
            .setHeader("password").simple("{{KIPA_SFTP_PASSWORD_P22}}")
            .setHeader("directoryPath").simple("{{KIPA_DIRECTORY_PATH_P22}}")
            .to("direct:fetchFileNamesFromSftp")
            //.to("direct:fetchDirectoriesFromSftp")
        ;


        from("direct:fetchFileNamesFromSftp")
            .bean(sftpProcessor, "getAllSFTPFileNames(*)")
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

        from("sftp:{{KIPA_SFTP_HOST}}:22/{{KIPA_DIRECTORY_PATH_P24}}?username={{KIPA_SFTP_USER_P24}}"
                + "&password={{KIPA_SFTP_PASSWORD_P24}}"
                + "&strictHostKeyChecking=no"
                + "&delay=30000"
                + "&noop=true"
                + "&antInclude=YA_p24_091_20241209110955_091_TOJT*" 
            )
            .autoStartup("{{TEST_SEND_JSONFILES_AUTOSTARTUP}}")
            .routeId("kipa-fetch-files")
            .log("Start route to fetch and send json files via email")
            .setHeader("messageSubject", simple("kipa json file"))
            .setHeader("emailMessage", simple("This is a test file from Kipa"))
            .log("Sending json file :: ${header.CamelFileName}")
            .bean(this, "sendJsonFileByEmail")
        ;

        from("sftp:{{KIPA_SFTP_HOST}}:22/{{KIPA_DIRECTORY_PATH_P22}}?username={{KIPA_SFTP_USER_P22}}"
                + "&password={{KIPA_SFTP_PASSWORD_P22}}"
                + "&strictHostKeyChecking=no"
                + "&delay=30000"
                + "&noop=true"
                + "&antInclude=YA_p22-091_202404*" 
            )
            .autoStartup("{{TEST_MOVE_FILES_AUTOSTARTUP}}")
            .routeId("kipa-move-files")
            .log("Start route to move kipa files another directory")
            .log("file name :: ${headers.CamelFileName}")
            .setVariable("kipa_dir").simple("processed")
            .pollEnrich()
                .simple("sftp:{{KIPA_SFTP_HOST}}:22/{{KIPA_DIRECTORY_PATH_P22}}?username={{KIPA_SFTP_USER_P22}}&password={{KIPA_SFTP_PASSWORD_P22}}&strictHostKeyChecking=no&fileName=${headers.CamelFileName}&move=../${variable.kipa_dir}")
                .timeout(10000)
            .log("CamelFtpReplyString: ${headers.CamelFtpReplyString}")
        ;

        from("{{TEST_QUARTZ_TIMER}}")
            .autoStartup("{{TEST_QUARTZ_TIMER_AUTOSTARTUP}}")
            .log("Starting the timer route")
            .process(exchange -> {
                if (redisProcessor.acquireLock(LOCK_KEY, 300)) { 
                    exchange.getIn().setHeader("lockAcquired", true);
                    System.out.println("Lock acquired, processing starts");

                } else {
                    exchange.getIn().setHeader("lockAcquired", false);
                    System.out.println("Lock not acquired, skipping processing");

                }
            })

            .filter(header("lockAcquired").isEqualTo(true)) 
                .log("Timer route triggered, start processing")
            .end()
            //.process(exchange -> releaseLock())
        ;

        from("timer://test?repeatCount=1&delay=5000")
            .log("Test user :: {{TEST_USER}}")
        ;
    }     
}
