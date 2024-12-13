package fi.hel.integration.ya;

import java.util.Properties;

import org.apache.camel.Exchange;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.Message;
import jakarta.mail.Multipart;

@ApplicationScoped
@Named("sendEmail")
public class SendEmail {

    @Inject
    Logger log; 

    //@ConfigProperty(name="MAKSULIIKENNE_EMAIL_RECIPIENTS", defaultValue = "recipient")
    //String recipients;

    @ConfigProperty(name="MAIL_SMTP_HOST", defaultValue = "host")
    String host;

    @ConfigProperty(name="MAIL_SMTP_PORT", defaultValue = "port")
    String port;

    @ConfigProperty(name="MAIL_SMTP_SENDER", defaultValue = "sender")
    String sender;


    public void sendEmail(Exchange ex) {
        try {
            
            //byte[] byteArray = ex.getIn().getBody(byte[].class);
            String messageSubject = (String) ex.getIn().getHeader("messageSubject");
            String emailMessage = (String) ex.getIn().getHeader("emailMessage");
            String recipients = (String) ex.getIn().getHeader("emailRecipients");
            System.out.println("Sending email to " + recipients);

            Properties prop = new Properties();
            prop.put("mail.smtp.starttls.enable", "true");
            prop.put("mail.smtp.host", host);
            prop.put("mail.smtp.port", port);

            Session session = Session.getInstance(prop);
    
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
            message.setSubject(messageSubject);
    
            Multipart multipart = new MimeMultipart();
    
            // Create a text part if you want to include a message in the email
            MimeBodyPart textBodyPart = new MimeBodyPart();
            textBodyPart.setText(emailMessage, "utf-8", "html");
            multipart.addBodyPart(textBodyPart);
    
            // Attach the file
            //MimeBodyPart attachmentBodyPart = new MimeBodyPart();
            //DataSource source = new ByteArrayDataSource(byteArray, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            //attachmentBodyPart.setDataHandler(new DataHandler(source));
            //attachmentBodyPart.setFileName(filename);
            //multipart.addBodyPart(attachmentBodyPart);
    
            // Set the content of the message
            message.setContent(multipart);
    
            // Send the email
            Transport.send(message);
        
        } catch (Exception e) {
            log.error(e);
            //e.printStackTrace();
            ex.setException(e);
        }
    }
}
