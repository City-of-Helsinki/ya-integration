package fi.hel.integration.ya;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RuntimeCamelException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;



@ApplicationScoped
@Named ("sftpProcessor")
public class SftpProcessor {

    @Inject
    ProducerTemplate producerTemplate;

    public List<String> getAllSFTPFileNames(Exchange ex) throws JSchException, SftpException, IOException {
        String directoryPath = ex.getIn().getHeader("directoryPath", String.class);
        String hostname = ex.getIn().getHeader("hostname", String.class);
        String username = ex.getIn().getHeader("username", String.class);
        String password = ex.getIn().getHeader("password", String.class);
        String privateKeyEncoded = ex.getIn().getHeader("privateKey", String.class);
        String privateKey = null;
        String filePrefix = ex.getIn().getHeader("filePrefix", String.class);
        String filePrefix2 = ex.getIn().getHeader("filePrefix2", String.class);
        
        if(privateKeyEncoded != null) {
           privateKey = new String(Base64.getDecoder().decode(privateKeyEncoded));
        }

        // Check for missing or invalid headers
        if (directoryPath == null || hostname == null || username == null || (password == null && privateKey == null)) {
            throw new IllegalArgumentException("Missing one or more required SFTP headers (directoryPath, hostname, username, and either password or privateKey.");
        }

        JSch jsch = new JSch();
        if (privateKey != null) {
            jsch.addIdentity("sftp-identity", privateKey.getBytes(), null, null);
        }

        Session session = jsch.getSession(username, hostname, 22);
        if (password != null) {
            session.setPassword(password);
        }
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        channelSftp.connect();

        List<String> fileNames = new ArrayList<>();
        Vector<ChannelSftp.LsEntry> files = channelSftp.ls(directoryPath);
        for (ChannelSftp.LsEntry file : files) {
            if (!file.getAttrs().isDir()) {
                String fileName = file.getFilename();

                if (filePrefix == null || filePrefix2 == null || 
                    fileName.startsWith(filePrefix) || fileName.startsWith(filePrefix2)) {
                    fileNames.add(fileName);
                    //System.out.println(fileName);
                }
            }        
        }

        channelSftp.exit();
        session.disconnect();

        return fileNames;
    }

    public void fetchAllFilesFromSftpByFileName(Exchange ex) {
        List<String> fileNames = ex.getIn().getBody(List.class);
        Map<String, Object> headers = ex.getIn().getHeaders();

        List<Map<String, Object>> combinedJsons = new ArrayList<>();

        for (String fileName : fileNames) {

            if (combinedJsons.size() >= 500) {
                System.out.println("Reached the maximum size of combinedJsons (500). Stopping further processing.");
                break;
            }
    
            Map<String, Object> result = producerTemplate.requestBodyAndHeaders("direct:poll-and-validate-file", fileName, headers, Map.class);

            Boolean isJsonValid = (Boolean) result.get("isJsonValid");
            if (isJsonValid != null && isJsonValid) {
                combinedJsons.add((Map<String, Object>) result.get("fileContent"));
            } else {
                String errorMessage = (String) result.get("errorMessage");
                System.out.println("Invalid JSON: " + errorMessage);
            }
        }

        //System.out.println(("combined jsons :: " + combinedJsons));
        ex.getIn().setBody(combinedJsons);
    }


    public void fetchFile(Exchange ex) {
        Session session = null;
        ChannelSftp channelSftp = null;

        String directoryPath = ex.getIn().getHeader("directoryPath", String.class);
        String hostname = ex.getIn().getHeader("hostname", String.class);
        String username = ex.getIn().getHeader("username", String.class);
        String password = ex.getIn().getHeader("password", String.class);
        String fileName = ex.getIn().getHeader("CamelFileName", String.class);
        
        try {

            JSch jsch = new JSch();
            session = jsch.getSession(username, hostname, 22);
            session.setPassword(password);

            // Disable strict host key checking
            session.setConfig("StrictHostKeyChecking", "no");

            System.out.println("Connecting to SFTP server...");
            session.connect();

            // Open SFTP channel
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            System.out.println("Connected to SFTP server");

            String remoteFilePath = directoryPath + "/" + fileName;

            InputStream inputStream = channelSftp.get(remoteFilePath);

            String jsonString = convertInputStreamToJson(inputStream);

            System.out.println("File fetched successfully: " + fileName);

            ex.getIn().setBody(jsonString);
            ex.getIn().setHeader("CamelFileName", fileName);

        } catch (JSchException | SftpException | IOException e) {
         
            throw new RuntimeCamelException("SFTP operation failed: " + e.getMessage(), e);    
        
        } finally {
            // Cleanup and close SFTP connection
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
            System.out.println("SFTP connection closed");
        }
    }

    private String convertInputStreamToJson(InputStream inputStream) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        // Convert InputStream directly to a JSON String
        return objectMapper.readTree(inputStream).toString();
    }

    public void moveFile(Exchange ex) {
        Session session = null;
        ChannelSftp channelSftp = null;

        String directoryPath = ex.getIn().getHeader("directoryPath", String.class);
        String hostname = ex.getIn().getHeader("hostname", String.class);
        String username = ex.getIn().getHeader("username", String.class);
        String password = ex.getIn().getHeader("password", String.class);
        String fileName = ex.getIn().getHeader("CamelFileName", String.class);
        String targetDir = ex.getIn().getHeader("targetDirectory", String.class);

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, hostname, 22);
            session.setPassword(password);

            session.setConfig("StrictHostKeyChecking", "no");

            System.out.println("Connecting to SFTP server...");
            session.connect();

            // Open SFTP channel
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            System.out.println("Connected to SFTP server");

            String sourcePath = directoryPath + "/" + fileName;
            String targetPath = targetDir + "/" + fileName;

            channelSftp.rename(sourcePath, targetPath);

            System.out.println("File moved successfully: " + fileName);

        } catch (JSchException | SftpException e) {
            e.printStackTrace();
        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
            System.out.println("SFTP connection closed");
        }
    }
}
