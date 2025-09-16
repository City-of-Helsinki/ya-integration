package fi.hel.integration.ya.maksuliikenne.Tasmaytysraportti;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.*;
import com.opencsv.CSVWriter;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.RuntimeCamelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class TasmaytysraporttiCombineProcessor implements Processor {

    private static final Logger LOG = LoggerFactory.getLogger(TasmaytysraporttiCombineProcessor.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Allowed claim types
    private static final Set<String> ALLOWED_CLAIM_TYPES = Set.of(
        "PALKKATUKI",
        "PALKKATUKI_55V",
        "JARJESTELYTUKI"
    );

    @Override
    public void process(Exchange exchange) throws Exception {
        // Get SFTP connection parameters from headers (same as SftpProcessor)
        String directoryPath = exchange.getIn().getHeader("directoryPath", String.class);
        String hostname = exchange.getIn().getHeader("hostname", String.class);
        String username = exchange.getIn().getHeader("username", String.class);
        String password = exchange.getIn().getHeader("password", String.class);
        String privateKeyEncoded = exchange.getIn().getHeader("privateKey", String.class);
        String privateKey = null;
        String filePrefix = exchange.getIn().getHeader("filePrefix", String.class);
        
        if(privateKeyEncoded != null) {
            privateKey = new String(Base64.getDecoder().decode(privateKeyEncoded));
        }
        
        // If no SFTP parameters, try local directory
        if (hostname == null || hostname.trim().isEmpty() || "null".equalsIgnoreCase(hostname) ||
            username == null || username.trim().isEmpty() || "null".equalsIgnoreCase(username)) {
            LOG.info("No SFTP parameters provided, processing local files");
            processLocalFiles(exchange);
            return;
        }
        
        LOG.info("Connecting to SFTP server {}:{} as user {}", hostname, 22, username);
        
        Session session = null;
        ChannelSftp channelSftp = null;
        List<Map<String, Object>> allRecords = new ArrayList<>();
        
        try {
            // Setup SFTP connection (same as SftpProcessor)
            JSch jsch = new JSch();
            
            if(privateKey != null) {
                jsch.addIdentity("", privateKey.getBytes(), null, null);
                session = jsch.getSession(username, hostname, 22);
            } else {
                session = jsch.getSession(username, hostname, 22);
                session.setPassword(password);
            }
            
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("kex", "ecdh-sha2-nistp384,ecdh-sha2-nistp256,diffie-hellman-group14-sha256,diffie-hellman-group16-sha512,diffie-hellman-group-exchange-sha256");
            config.put("server_host_key", "ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ed25519");
            session.setConfig(config);
            
            session.connect();
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            
            LOG.info("Connected to SFTP server, listing files in {}", directoryPath);
            
            // List and process JSON files
            @SuppressWarnings("unchecked")
            Vector<ChannelSftp.LsEntry> files = channelSftp.ls(directoryPath);
            
            int processedFiles = 0;
            String minDate = null;
            String maxDate = null;
            
            for (ChannelSftp.LsEntry file : files) {
                if (!file.getAttrs().isDir()) {
                    String fileName = file.getFilename();
                    
                    // Check if file matches criteria
                    if (fileName.toLowerCase().endsWith(".json") && 
                        (filePrefix == null || fileName.startsWith(filePrefix))) {
                        
                        LOG.info("Processing file: {}", fileName);
                        
                        // Extract date from filename (format: YATE_tasmaytysraportti_p24-02012566_YYYYMMDD.json)
                        String fileDate = extractDateFromFileName(fileName);
                        if (fileDate != null) {
                            if (minDate == null || fileDate.compareTo(minDate) < 0) {
                                minDate = fileDate;
                            }
                            if (maxDate == null || fileDate.compareTo(maxDate) > 0) {
                                maxDate = fileDate;
                            }
                        }
                        
                        try {
                            // Fetch file content
                            String remoteFilePath = directoryPath + "/" + fileName;
                            InputStream inputStream = channelSftp.get(remoteFilePath);
                            String jsonContent = convertInputStreamToString(inputStream);
                            
                            // Parse JSON
                            List<Map<String, Object>> fileRecords = objectMapper.readValue(jsonContent, 
                                new TypeReference<List<Map<String, Object>>>() {});
                            
                            // Filter by claimType
                            List<Map<String, Object>> filteredRecords = fileRecords.stream()
                                .filter(record -> {
                                    Object claimType = record.get("claimType");
                                    return claimType != null && ALLOWED_CLAIM_TYPES.contains(claimType.toString());
                                })
                                .collect(Collectors.toList());
                            
                            LOG.info("File {} had {} records, {} after filtering", 
                                fileName, fileRecords.size(), filteredRecords.size());
                            
                            allRecords.addAll(filteredRecords);
                            processedFiles++;
                            
                        } catch (Exception e) {
                            LOG.error("Error processing file {}: {}", fileName, e.getMessage());
                        }
                    }
                }
            }
            
            LOG.info("Processed {} JSON files, total records: {}", processedFiles, allRecords.size());
            
            if (allRecords.isEmpty()) {
                LOG.warn("No records with allowed claim types found in any file");
                exchange.getIn().setBody("");
                return;
            }
            
            // Convert to CSV
            String csv = convertToCSV(allRecords);
            exchange.getIn().setBody(csv);
            
            // Generate timestamp for filename
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            
            // Use extracted dates or defaults
            String startDate = (minDate != null) ? minDate : "00000000";
            String endDate = (maxDate != null) ? maxDate : "99999999";
            
            String fileName = String.format("tasmaytysraportti_%s_%s_%s.csv", startDate, endDate, timestamp);
            exchange.getIn().setHeader(Exchange.FILE_NAME, fileName);
            
            LOG.info("Successfully created combined CSV with {} records", allRecords.size());
            
        } catch (JSchException | SftpException e) {
            LOG.error("SFTP operation failed: {}", e.getMessage());
            throw new RuntimeCamelException("SFTP operation failed: " + e.getMessage(), e);
        } finally {
            // Cleanup
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
            LOG.info("SFTP connection closed");
        }
    }
    
    private void processLocalFiles(Exchange exchange) throws Exception {
        String inputDir = "inbox/täsmäytysraportit";
        LOG.info("Processing local JSON files from {}", inputDir);
        
        File directory = new File(inputDir);
        if (!directory.exists() || !directory.isDirectory()) {
            LOG.warn("Directory {} does not exist or is not a directory", inputDir);
            exchange.getIn().setBody("");
            return;
        }
        
        File[] jsonFiles = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        
        if (jsonFiles == null || jsonFiles.length == 0) {
            LOG.warn("No JSON files found in {}", inputDir);
            exchange.getIn().setBody("");
            return;
        }
        
        LOG.info("Found {} JSON files to process", jsonFiles.length);
        
        List<Map<String, Object>> allRecords = new ArrayList<>();
        String minDate = null;
        String maxDate = null;
        
        for (File jsonFile : jsonFiles) {
            LOG.info("Processing file: {}", jsonFile.getName());
            
            // Extract date from filename
            String fileDate = extractDateFromFileName(jsonFile.getName());
            if (fileDate != null) {
                if (minDate == null || fileDate.compareTo(minDate) < 0) {
                    minDate = fileDate;
                }
                if (maxDate == null || fileDate.compareTo(maxDate) > 0) {
                    maxDate = fileDate;
                }
            }
            
            try {
                String content = new String(java.nio.file.Files.readAllBytes(jsonFile.toPath()));
                List<Map<String, Object>> fileRecords = objectMapper.readValue(content, 
                    new TypeReference<List<Map<String, Object>>>() {});
                
                // Filter by claimType
                List<Map<String, Object>> filteredRecords = fileRecords.stream()
                    .filter(record -> {
                        Object claimType = record.get("claimType");
                        return claimType != null && ALLOWED_CLAIM_TYPES.contains(claimType.toString());
                    })
                    .collect(Collectors.toList());
                
                LOG.info("File {} had {} records, {} after filtering", 
                    jsonFile.getName(), fileRecords.size(), filteredRecords.size());
                
                allRecords.addAll(filteredRecords);
                
            } catch (Exception e) {
                LOG.error("Error processing file {}: {}", jsonFile.getName(), e.getMessage());
            }
        }
        
        LOG.info("Total records collected from all files: {}", allRecords.size());
        
        if (allRecords.isEmpty()) {
            LOG.warn("No records with allowed claim types found in any file");
            exchange.getIn().setBody("");
            return;
        }
        
        // Convert to CSV
        String csv = convertToCSV(allRecords);
        exchange.getIn().setBody(csv);
        
        // Generate timestamp for filename
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        
        // Use extracted dates or defaults
        String startDate = (minDate != null) ? minDate : "00000000";
        String endDate = (maxDate != null) ? maxDate : "99999999";
        
        String fileName = String.format("tasmaytysraportti_%s_%s_%s.csv", startDate, endDate, timestamp);
        exchange.getIn().setHeader(Exchange.FILE_NAME, fileName);
        
        LOG.info("Successfully created combined CSV with {} records", allRecords.size());
    }
    
    private String convertInputStreamToString(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            return stringBuilder.toString();
        }
    }

    private String convertToCSV(List<Map<String, Object>> jsonData) throws Exception {
        if (jsonData == null || jsonData.isEmpty()) {
            return "";
        }
        
        StringWriter stringWriter = new StringWriter();
        
        // Use semicolon as separator
        try (CSVWriter csvWriter = new CSVWriter(stringWriter, ';', 
                CSVWriter.DEFAULT_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {
            Set<String> allKeys = collectAllKeys(jsonData);
            List<String> headers = new ArrayList<>(allKeys);
            Collections.sort(headers);
            
            // Write header row
            csvWriter.writeNext(headers.toArray(new String[0]));
            
            // Write data rows
            for (Map<String, Object> record : jsonData) {
                String[] row = new String[headers.size()];
                for (int i = 0; i < headers.size(); i++) {
                    Object value = getNestedValue(record, headers.get(i));
                    row[i] = value != null ? value.toString() : "";
                }
                csvWriter.writeNext(row);
            }
        }
        
        return stringWriter.toString();
    }

    private Set<String> collectAllKeys(List<Map<String, Object>> jsonData) {
        Set<String> allKeys = new LinkedHashSet<>();
        
        for (Map<String, Object> record : jsonData) {
            collectKeysRecursively("", record, allKeys);
        }
        
        return allKeys;
    }

    private void collectKeysRecursively(String prefix, Map<String, Object> map, Set<String> keys) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            
            if (entry.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
                collectKeysRecursively(key, nestedMap, keys);
            } else {
                keys.add(key);
            }
        }
    }

    private Object getNestedValue(Map<String, Object> map, String key) {
        String[] parts = key.split("\\.");
        Object current = map;
        
        for (String part : parts) {
            if (current instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> currentMap = (Map<String, Object>) current;
                current = currentMap.get(part);
                if (current == null) {
                    return null;
                }
            } else {
                return null;
            }
        }
        
        return current;
    }
    
    private String extractDateFromFileName(String fileName) {
        // Expected format: YATE_tasmaytysraportti_p24-02012566_YYYYMMDD.json
        // or: tasmaytysraportti_YYYYMMDD.json
        try {
            if (fileName.endsWith(".json")) {
                String nameWithoutExt = fileName.substring(0, fileName.length() - 5);
                // Get the last part after underscore
                int lastUnderscore = nameWithoutExt.lastIndexOf('_');
                if (lastUnderscore > -1 && lastUnderscore < nameWithoutExt.length() - 1) {
                    String datePart = nameWithoutExt.substring(lastUnderscore + 1);
                    // Validate it's a date (8 digits)
                    if (datePart.matches("\\d{8}")) {
                        return datePart;
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not extract date from filename: {}", fileName);
        }
        return null;
    }
}