package fi.hel.integration.ya.starttiraha.processor;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.Exchange;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import fi.hel.integration.ya.Utils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;


@ApplicationScoped
@Named ("srProcessor")
public class StarttirahaProcessor {

    @Inject
    Logger log;

    @Inject
    Utils utils;

    @ConfigProperty(name = "STARTTIRAHA_PALVELUSUHDE_TUNNUS", defaultValue = "contactPerson")
    String id;

    private final String EMPTY = "";
    
    @SuppressWarnings("unchecked")
    public void createPersonalInfoMap(Exchange ex) {
        try {
            List<Map<String,Object>> body = ex.getIn().getBody(List.class);
            List<Map<String,Object>> personalInfoMapList = new ArrayList<>();

            for(Map<String,Object> item: body) {
            
                Map<String,Object> personalInfoMap = new LinkedHashMap<>();
            
                Map<String,Object> receiver = (Map<String, Object>) item.get("receiver");
                String socialSecurityNo = (String) receiver.get("socialSecurityNumber");
                String day = socialSecurityNo.substring(0, 2);
                String month = socialSecurityNo.substring(2, 4);
                String year = socialSecurityNo.substring(4, 6);
                String mark = socialSecurityNo.substring(6, 7);
            
                // 1900-luvulla syntyneillä välimerkki sotussa on '-' tai 'Y'
                // 2000-luvulla syntyneillä 'A'
                if (mark.equals("-") || mark.equals("Y")) {
                    year = "19" + year;
            
                } else {
                    year = "20" + year;
                }
        
                // muoto pp.kk.vvvv
                String dateOfBirth = day + "." + month + "." + year;

                String personalId = socialSecurityNo.substring(6);

                Map<String,Object> name = (Map<String, Object>) receiver.get("name");
                String lastName = (String) name.get("lastName");
                String firstName = (String) name.get("firstName");
            
                Map<String,Object> postalAddress = (Map<String, Object>) receiver.get("postalAddress");
                ArrayList<String> addressList = (ArrayList<String>) postalAddress.get("addressLine");
                String address = addressList.get(0);
            
                String postalCode = (String) postalAddress.get("postalCode");
                String postOffice = (String) postalAddress.get("postOffice");
                String postalCodeAndOffice = postalCode + " " + postOffice;            
            
                Map<String,Object> bankAccount = (Map<String, Object>) receiver.get("bankAccount");
                String bankAccountNo = (String) bankAccount.get("value");
            
                String paymentPeriodStartDate = (String) item.get("paymentPeriodStartDate");
                String paymentPeriodEndDate = (String) item.get("paymentPeriodEndDate");

                if(paymentPeriodStartDate != null) {
                    paymentPeriodStartDate = utils.convertDate(paymentPeriodStartDate, "yyyy-MM-dd HH:mm:ss", "dd.MM.yyyy");
            
                } else {
                    paymentPeriodStartDate = null;
                }

                if(paymentPeriodEndDate != null) {
                    paymentPeriodEndDate = utils.convertDate(paymentPeriodEndDate, "yyyy-MM-dd HH:mm:ss", "dd.MM.yyyy");
            
                } else {
                    paymentPeriodEndDate= null;
                }
            
                personalInfoMap.put("id", id);
                personalInfoMap.put("personalNumber", EMPTY);
                personalInfoMap.put("dateOfBirth", dateOfBirth);
                personalInfoMap.put("personalId", personalId);
                personalInfoMap.put("lastName", lastName);
                personalInfoMap.put("firstName", firstName);
                personalInfoMap.put("name1", EMPTY);
                personalInfoMap.put("name2", EMPTY);
                personalInfoMap.put("nativeLanguage", EMPTY);
                personalInfoMap.put("language", EMPTY);
                personalInfoMap.put("address", address);
                personalInfoMap.put("postalCodeAndOffice", postalCodeAndOffice);
                personalInfoMap.put("bankAccountNo", bankAccountNo);
                personalInfoMap.put("bic1", EMPTY);
                personalInfoMap.put("deliveryMethod", EMPTY);
                personalInfoMap.put("paymentPeriodStartDate", paymentPeriodStartDate);
                personalInfoMap.put("continuityDate", EMPTY);
                personalInfoMap.put("task", EMPTY);
                personalInfoMap.put("group", EMPTY);
                personalInfoMap.put("leaveProcessing", EMPTY);
                personalInfoMap.put("pensionSystem", EMPTY);
                personalInfoMap.put("accidentInsuranceCompany", EMPTY);
                personalInfoMap.put("insuranceException", EMPTY);
                personalInfoMap.put("paymentPeriodEndDate", paymentPeriodEndDate);
                personalInfoMap.put("terminationReason", EMPTY);
                personalInfoMap.put("companyId", EMPTY);
                personalInfoMap.put("internalOrder", EMPTY);
                personalInfoMap.put("paymentDueDate", EMPTY);
                personalInfoMap.put("paymentType", EMPTY);
                personalInfoMapList.add(personalInfoMap);
            }

            ex.getIn().setBody(personalInfoMapList);
        
        } catch (Exception e){
            log.error(e);
            e.printStackTrace();
            ex.setException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public void createPayrollTransactionMap(Exchange ex) {
        try {
            List<Map<String,Object>> body = ex.getIn().getBody(List.class);
            List<Map<String,Object>> payrollTransactionMapList = new ArrayList<>();

            for(Map<String,Object> item: body) {
                Map<String,Object> payrollTransactionMap = new LinkedHashMap<>();
                
                Map<String,Object> receiver = (Map<String, Object>) item.get("receiver");
                String socialSecurityNo = (String) receiver.get("socialSecurityNumber");
            
                String paymentPeriodStartDate = (String) item.get("paymentPeriodStartDate");
                String paymentPeriodEndDate = (String) item.get("paymentPeriodEndDate");
                if(paymentPeriodStartDate != null) {
                    paymentPeriodStartDate = utils.convertDate(paymentPeriodStartDate, "yyyy-MM-dd HH:mm:ss", "dd.MM.yyyy");
            
                } else {
                    paymentPeriodStartDate = null;
                }

                if(paymentPeriodEndDate != null) {
                    paymentPeriodEndDate = utils.convertDate(paymentPeriodEndDate, "yyyy-MM-dd HH:mm:ss", "dd.MM.yyyy");
            
                } else {
                    paymentPeriodEndDate= null;
                }
                
                double grossSum = (double) item.get("grossSum");
                String grossSumString = String.valueOf(grossSum).replace('.', ',');
    
                Map<String,Object> view = (Map<String, Object>) item.get("view");
                String grantDecisionRecordNumber = (String) view.get("grantDecisionRecordNumber");

                payrollTransactionMap.put("socialSecurityNo", socialSecurityNo);
                payrollTransactionMap.put("paymentPeriodStartDate", paymentPeriodStartDate);
                payrollTransactionMap.put("paymentPeriodEndDate", paymentPeriodEndDate);
                payrollTransactionMap.put("payType", EMPTY);
                payrollTransactionMap.put("grossSum", grossSumString);
                payrollTransactionMap.put("compantyId", EMPTY);
                payrollTransactionMap.put("internalOrder", EMPTY);
                payrollTransactionMap.put("grantDecisionRecordNumber", grantDecisionRecordNumber);
                payrollTransactionMapList.add(payrollTransactionMap);
            }

            ex.getIn().setBody(payrollTransactionMapList);

        } catch (Exception e){
            log.error(e);
            //e.printStackTrace();
            ex.setException(e);
        }
    }

    public void writeFileSftp(Exchange ex) {
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            String hostname = ex.getIn().getHeader("hostname", String.class);
            String username = ex.getIn().getHeader("username", String.class);
            String privateKeyEncoded = ex.getIn().getHeader("privateKey", String.class);
            String directoryPath = ex.getIn().getHeader("directoryPath", String.class);
            String fileName = ex.getIn().getHeader(Exchange.FILE_NAME, String.class);
            String body = ex.getIn().getBody(String.class);

            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyEncoded);

            JSch jsch = new JSch();
            jsch.addIdentity("privateKey", privateKeyBytes, null, null);

            session = jsch.getSession(username, hostname, 22);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no"); // Disable host key checking for simplicity
            session.setConfig(config);
            session.connect();

            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            String remoteFilePath = directoryPath + "/" + fileName;
            try (ByteArrayInputStream fileStream = new ByteArrayInputStream(body.getBytes())) {
                channelSftp.put(fileStream, remoteFilePath);
            }

            ex.getMessage().setHeader("CamelFtpReplyCode", "200");
            ex.getMessage().setHeader("CamelFtpReplyString", "File uploaded successfully");

            log.infof("File uploaded successfully to %s: %s", directoryPath, fileName);

        } catch (Exception e) {
            log.error("Error during SFTP upload: {}", e.getMessage(), e);
            ex.setException(e);
            ex.getMessage().setHeader("CamelFtpReplyCode", "500");
            ex.getMessage().setHeader("CamelFtpReplyString", "Error during SFTP upload");
    
        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }
}
   