package fi.hel.integration.ya.maksuliikenne.processor;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.Exchange;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import fi.hel.integration.ya.Utils;
import fi.hel.integration.ya.maksuliikenne.models.kirjanpitoSAP.LineItemType;
import fi.hel.integration.ya.maksuliikenne.models.kirjanpitoSAP.SBO_SimpleAccounting;
import fi.hel.integration.ya.maksuliikenne.models.kirjanpitoSAP.SBO_SimpleAccountingContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named ("kpProcessor")
public class KirjanpitoProcessor {
    
    @Inject
    Logger log;

    @Inject 
    Utils utils;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_SISAINENTILAUS_PT", defaultValue= "orderItemNumberPt")
    String orderItemNumberPt;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_SISAINENTILAUS_PT55", defaultValue= "orderItemNumberPt55")
    String orderItemNumberPt55;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_SISAINENTILAUS_HKK", defaultValue= "orderItemNumberHkk")
    String orderItemNumberHkk;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_SISAINENTILAUS_MYK", defaultValue= "orderItemNumberMyk")
    String orderItemNumberMyk;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_SISAINENTILAUS_TOJT", defaultValue= "orderItemNumberTojt")
    String orderItemNumberTojt;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_PAAKIRJATILI_PT", defaultValue= "glAccountPt")
    String glAccountPt;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_PAAKIRJATILI_PT55", defaultValue= "glAccountPt55")
    String glAccountPt55;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_PAAKIRJATILI_HKK", defaultValue= "glAccountHkk")
    String glAccountHkk;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_PAAKIRJATILI_MYK", defaultValue= "glAccountMyk")
    String glAccountMyk;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_PAAKIRJATILI_TOJT", defaultValue= "glAccountTojt")
    String glAccountTojt;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_PAAKIRJATILI_KREDIT", defaultValue= "glAccountCredit")
    String glAccountCredit;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_COMPANYCODE", defaultValue= "companyCode")
    String companyCode;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_DOCUMENTTYPE", defaultValue= "documentType")
    String documentType;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_TULOSYKSIKKO", defaultValue= "profitCenter")
    String profitCenter;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_SENDERID", defaultValue= "senderId")
    String senderId;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_HKI_BUSINESSID", defaultValue= "hkiBusinessId")
    String hkiBusinessId;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_HKI_PARTNERCODE", defaultValue= "1234")
    String hkiPartnerCode;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_SOTEPEKREDIT_SISAINENTILAUS", defaultValue= "12345")
    String orderItemSotepeCredit;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_SOTEPEKREDIT_PAAKIRJATILI", defaultValue= "12345")
    String glAccountSotepeCredit;

    private static final String EMPTY = "";
    
    // Claim types:
    private static final String PT = "PALKKATUKI";
    private static final String PT55 = "PALKKATUKI_55V";
    private static final String HKK = "HARKINNANVARAINEN_KULUKORVAUS";
    private static final String MYK = "MATKA_JA_YOPYMISKUSTANNUSTENKORVAUS";
    private static final String TOJT = "TYOOLOSUHTEIDEN_JARJESTELYTUKI";
    private static final String PARTNERCODE_FILE_PATH = "sap/kumppanikoodilistaus 16.8.2024.xlsx";

    @SuppressWarnings("unchecked")
    public void mapAccountigData(Exchange ex) {
        
        try {

            SBO_SimpleAccountingContainer simpleAccountingcontainer = new SBO_SimpleAccountingContainer();
            SBO_SimpleAccounting simpleAccounting = new SBO_SimpleAccounting();
            LineItemType lineItemTypeDebit = new LineItemType();
            LineItemType lineItemTypeCredit = new LineItemType();
            List<LineItemType> lineItemTypes = new ArrayList<>();
            List<SBO_SimpleAccounting> sbo_SimpleAccountings = new ArrayList<>();
            
            Map<String,Object> body = ex.getIn().getBody(Map.class);
            //System.out.println("Accounting data map :: " + body);
            String claimType = (String) body.get("claimType");
            simpleAccounting.setSenderId(senderId);
            simpleAccounting.setCompanyCode(companyCode);
            simpleAccounting.setDocumentType(documentType);

            String date = "20251231"; //utils.getCurrentTime("yyyyMMdd");
            log.info("Generated date for accounting: " + date);
            simpleAccounting.setDocumentDate(date);
            simpleAccounting.setPostingDate(date);
            
            // oltava numeerinen arvo ja 9 merkkiä, tarvittaessa täytetään etunollilla
            Map<String, Object> delivery = (Map<String, Object>) body.get("delivery");
            int id = (int) delivery.get("id");
            // Convert the integer ID to a string with leading zeros to ensure it is 9 characters long
            String formattedId = String.format("%09d", id);
            simpleAccounting.setReference(formattedId);

            String header = claimType;

            // The maximum number of characters in the header is 25
            if(header.length() > 25) {
                header = header.substring(0, 25);
            }

            simpleAccounting.setHeaderText(header);

            String currency = (String) body.get("currency");
            simpleAccounting.setCurrencyCode(currency);

            Map<String, Object> posting = (Map<String, Object>) body.get("posting");
            List<Map<String, Object>> postingInstallment = (List<Map<String, Object>>) posting.get("postingInstallment");
            String vatCode = (String) postingInstallment.get(0).get("vatCode");
            
            if(vatCode.length() >= 2) {
                vatCode = vatCode.substring(vatCode.length() - 2);
            }

            // Debit row:
            lineItemTypeDebit.setTaxCode(vatCode);

            double sum = (double) body.get("grossSum");
            String sumAsString = String.valueOf(sum);
            
            sumAsString = sumAsString.replaceAll("\\.", ",");
            lineItemTypeDebit.setAmountInDocumentCurrency(sumAsString);

            String ourRefence = (String) body.get("ourReference");
            String cutOurReference = ourRefence.length() > 50 ? ourRefence.substring(0, 50) : ourRefence;

            lineItemTypeDebit.setLineText(cutOurReference);


            // Kumppanikoodi
            Map<String, Object> receiver = (Map<String, Object>) body.get("receiver");
            String businessId = (String) receiver.get("businessId");
            String partnerCode = getPartnerCode(businessId, PARTNERCODE_FILE_PATH);

            if (partnerCode == null || partnerCode.isEmpty()) {
                lineItemTypeDebit.setTradingPartner(EMPTY);
            } else {
                lineItemTypeDebit.setTradingPartner(partnerCode);
            } 

            // kirjaustunniste (sisäinen tilaus), pituus 10 numeroa, pakollinen
            // mäpätään claimTypen perusteella
            String orderItemNumber = getOrderItemNumber(claimType);
            lineItemTypeDebit.setOrderItemNumber(orderItemNumber);
            
            // SAP pääkirjatili (6 numeroa) -> pääkirjatili (tulos), pakollinen
            // mäpätään claimTypen perusteella
            String glAccount = getGlAccount(claimType);
            lineItemTypeDebit.setGlAccount(glAccount);

            // Credit row:
            lineItemTypeCredit.setAmountInDocumentCurrency("-" + sumAsString);
            lineItemTypeCredit.setLineText(cutOurReference);
            lineItemTypeCredit.setGlAccount(glAccountCredit);
            // SAP- tulosyksikkö (7 numeroa), pakollinen
            lineItemTypeCredit.setProfitCenter(profitCenter);


            lineItemTypes.add(lineItemTypeDebit);
            lineItemTypes.add(lineItemTypeCredit);
            simpleAccounting.setLineItem(lineItemTypes);
            sbo_SimpleAccountings.add(simpleAccounting);
            simpleAccountingcontainer.setSboSimpleAccounting(sbo_SimpleAccountings);

            ex.getIn().setBody(simpleAccountingcontainer);

            String fileName = (String) delivery.get("fileName");
            ex.getIn().setHeader("jsonFileName", fileName);

        } catch (Exception e){
            log.error(e);
            e.printStackTrace();
            ex.setException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public void mapAccountigDataSotepe(Exchange ex) {
        try {
            SBO_SimpleAccountingContainer simpleAccountingcontainer = new SBO_SimpleAccountingContainer();
            SBO_SimpleAccounting simpleAccounting = new SBO_SimpleAccounting();
            LineItemType lineItemTypeDebit = new LineItemType();
            LineItemType lineItemTypeCredit = new LineItemType();
            List<LineItemType> lineItemTypes = new ArrayList<>();
            List<SBO_SimpleAccounting> sbo_SimpleAccountings = new ArrayList<>();
            
            Map<String,Object> body = ex.getIn().getBody(Map.class);
            //System.out.println("Accounting data map :: " + body);
            String claimType = (String) body.get("claimType");
            simpleAccounting.setSenderId(senderId);
            simpleAccounting.setCompanyCode(companyCode);
            simpleAccounting.setDocumentType(documentType);

            String date = utils.getCurrentTime("YYYYMMdd");
            simpleAccounting.setDocumentDate(date);
            simpleAccounting.setPostingDate(date);
            
            // oltava numeerinen arvo ja 9 merkkiä, tarvittaessa täytetään etunollilla
            Map<String, Object> delivery = (Map<String, Object>) body.get("delivery");
            int id = (int) delivery.get("id");
            // Convert the integer ID to a string with leading zeros to ensure it is 9 characters long
            String formattedId = String.format("%09d", id);
            simpleAccounting.setReference(formattedId);

            String header = claimType;

            // The maximum number of characters in the header is 25
            if(header.length() > 25) {
                header = header.substring(0, 25);
            }

            simpleAccounting.setHeaderText(header);

            String currency = (String) body.get("currency");
            simpleAccounting.setCurrencyCode(currency);

            Map<String, Object> posting = (Map<String, Object>) body.get("posting");
            List<Map<String, Object>> postingInstallment = (List<Map<String, Object>>) posting.get("postingInstallment");
            String vatCode = (String) postingInstallment.get(0).get("vatCode");
            
            if(vatCode.length() >= 2) {
                vatCode = vatCode.substring(vatCode.length() - 2);
            }

            // Debit row:
            lineItemTypeDebit.setTaxCode(vatCode);

            double sum = (double) body.get("grossSum");
            String sumAsString = String.valueOf(sum);
            
            sumAsString = sumAsString.replaceAll("\\.", ",");
            lineItemTypeDebit.setAmountInDocumentCurrency(sumAsString);

            String ourRefence = (String) body.get("ourReference");
            String cutOurReference = ourRefence.length() > 50 ? ourRefence.substring(0, 50) : ourRefence;
            lineItemTypeDebit.setLineText(cutOurReference);

            lineItemTypeDebit.setCompanyCode(companyCode);
            lineItemTypeDebit.setTradingPartner(hkiPartnerCode);

            // kirjaustunniste (sisäinen tilaus), pituus 10 numeroa, pakollinen
            // mäpätään claimTypen perusteella
            String orderItemNumber = getOrderItemNumber(claimType);
            lineItemTypeDebit.setOrderItemNumber(orderItemNumber);
            
            // SAP pääkirjatili (6 numeroa) -> pääkirjatili (tulos), pakollinen
            // mäpätään claimTypen perusteella
            String glAccount = getGlAccount(claimType);
            lineItemTypeDebit.setGlAccount(glAccount);

            // Credit row:
            lineItemTypeCredit.setAmountInDocumentCurrency("-" + sumAsString);
            lineItemTypeCredit.setLineText(cutOurReference);
            lineItemTypeCredit.setCompanyCode(hkiPartnerCode);
            lineItemTypeCredit.setTradingPartner(companyCode);
            lineItemTypeCredit.setOrderItemNumber(orderItemSotepeCredit);
            lineItemTypeCredit.setGlAccount(glAccountSotepeCredit);
            
            // SAP- tulosyksikkö (7 numeroa), pakollinen
            //lineItemTypeCredit.setProfitCenter(profitCenter);

            lineItemTypes.add(lineItemTypeDebit);
            lineItemTypes.add(lineItemTypeCredit);
            simpleAccounting.setLineItem(lineItemTypes);
            sbo_SimpleAccountings.add(simpleAccounting);
            simpleAccountingcontainer.setSboSimpleAccounting(sbo_SimpleAccountings);

            ex.getIn().setBody(simpleAccountingcontainer);

            String fileName = (String) delivery.get("fileName");
            ex.getIn().setHeader("jsonFileName", fileName);

        } catch (Exception e){
            log.error(e);
            e.printStackTrace();
            ex.setException(e);
        }
    }

    public String getPartnerCode(String businessId, String filePath) {
        try {
            InputStream fileStream = loadFileFromClasspath(filePath);
        
            Map<String, String> kumppanikoodit = readExcelFile(fileStream);
            String partnerCode = "";

            if(businessId != null && businessId.equals(hkiBusinessId)) {
                partnerCode = hkiPartnerCode;
            } else {
                partnerCode = kumppanikoodit.get(businessId);
            }
            
            return partnerCode;

        } catch (IOException e) {
            System.err.println("Error loading file from classpath: " + e.getMessage());
            e.printStackTrace();
            return null; 
        }
    }

    private static Map<String, String> readExcelFile(InputStream inputStream) {
        Map<String, String> kumppanikoodit = new HashMap<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                Cell ytunnusCell = row.getCell(0); // Y-tunnus is in the 1st column
                Cell kumppanikoodiCell = row.getCell(1); // kumppanikoodi is in the 2nd column

                if (ytunnusCell != null && kumppanikoodiCell != null) {
                    String ytunnus = ytunnusCell.getStringCellValue().trim();
                    String kumppanikoodi = getCellValueAsString(kumppanikoodiCell);
                    kumppanikoodit.put(ytunnus, kumppanikoodi);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return kumppanikoodit;
    }

    private static String getCellValueAsString(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
            double numericValue = cell.getNumericCellValue();
            if (numericValue == (long) numericValue) {
                // If the numeric value is an integer, cast to long and return as string
                return String.valueOf((long) numericValue);
            } else {
                // Otherwise, return the value as it is, including decimals
                return String.valueOf(numericValue);
            }
            default:
                return "";
        }
        
    }

    private String getOrderItemNumber(String claimType) {
            String orderItemNumber = "";

            switch(claimType) {
                case PT: 
                    orderItemNumber = orderItemNumberPt;
                    break;   
                
                case PT55:
                    orderItemNumber = orderItemNumberPt55;
                    break;

                case HKK:
                    orderItemNumber = orderItemNumberHkk;
                    break;

                case MYK:
                    orderItemNumber = orderItemNumberMyk;
                    break;
                
                case TOJT:
                    orderItemNumber = orderItemNumberTojt;
                    break;
            }

            return orderItemNumber;
    }

    private String getGlAccount(String claimType) {
        String glAccount = "";

        switch(claimType) {
            case PT: 
                glAccount = glAccountPt;
                break;   
            
            case PT55:
                glAccount = glAccountPt55;
                break;

            case HKK:
                glAccount = glAccountHkk;
                break;

            case MYK:
                glAccount = glAccountMyk;
                break;
            
            case TOJT:
                glAccount = glAccountTojt;
                break;
        }

        return glAccount;

    }

    private InputStream loadFileFromClasspath(String filePath) throws IOException {
        // Get the file from the classpath (from "sap" folder)
        InputStream fileInputStream = getClass().getClassLoader().getResourceAsStream(filePath);
        if (fileInputStream == null) {
            throw new FileNotFoundException("File not found in classpath: " + filePath);
        }
        return fileInputStream;
    }

    public void calculateKirjanpitoTotalAmounts(Exchange ex) {

        List<Map<String,Object>> body = ex.getIn().getBody(List.class);
        Map<String,Object> totalAmounts = new LinkedHashMap<>();
        int numberOfPmts = 0;
        BigDecimal totalSumOfPmts = new BigDecimal(0);
        int numberOfSotepePmts = 0;
        BigDecimal totalSumOfSotepePmts = new BigDecimal(0);


        for(Map<String,Object> payment: body) {

            double grossSum = (double) payment.get("grossSum");
            BigDecimal paymentSum = BigDecimal.valueOf(grossSum);
            totalSumOfPmts = totalSumOfPmts.add(paymentSum);
            numberOfPmts+=1;

            Map<String, Object> receiver = (Map<String, Object>) payment.get("receiver");
            String businessId = (String) receiver.get("businessId");

            if(businessId != null && !businessId.isEmpty() && businessId.equals(hkiBusinessId)) {
                totalSumOfSotepePmts = totalSumOfSotepePmts.add(paymentSum);
                numberOfSotepePmts+=1;
            }
        }

        totalAmounts.put("numberOfPmtsKirjanpito", numberOfPmts);
        totalAmounts.put("totalSumOfPmtsKirjanpito", totalSumOfPmts);
        totalAmounts.put("numberOfPmtsSotepe", numberOfSotepePmts);
        totalAmounts.put("totalSumOfPmtsSotepe", totalSumOfSotepePmts);

        ex.getIn().setHeader("reportDataKirjanpito", totalAmounts);
    }
    
    public void writeFileSapSftp(Exchange ex) {
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            // Retrieve headers and body
            String hostname = ex.getIn().getHeader("hostname", String.class);
            String username = ex.getIn().getHeader("username", String.class);
            String password = ex.getIn().getHeader("password", String.class); 
            String directoryPath = ex.getIn().getHeader("directoryPath", String.class);
            String fileName = ex.getIn().getHeader(Exchange.FILE_NAME, String.class);
            String body = ex.getIn().getBody(String.class);

            // Initialize JSch and set the private key
            JSch jsch = new JSch();

            // Create and configure the session
            session = jsch.getSession(username, hostname, 22);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("kex", "diffie-hellman-group1-sha1,diffie-hellman-group14-sha1");
            config.put("server_host_key", "ssh-rsa");

            session.setConfig(config);
            session.setPassword(password);  
            session.connect();

            // Open an SFTP channel
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // Upload the file as an InputStream
            String remoteFilePath = directoryPath + fileName;
            try (ByteArrayInputStream fileStream = new ByteArrayInputStream(body.getBytes())) {
                channelSftp.put(fileStream, remoteFilePath);
            }

            ex.getIn().setHeader("CamelFtpReplyCode", "200");
            ex.getIn().setHeader("CamelFtpReplyString", "File uploaded successfully");

            log.infof("File uploaded successfully to %s: %s", directoryPath, fileName);

        } catch (Exception e) {
            log.error("Error during SFTP upload: {}", e.getMessage(), e);
            ex.setException(e);
            ex.getIn().setHeader("CamelFtpReplyCode", "500");
            ex.getIn().setHeader("CamelFtpReplyString", "Error during SFTP upload");
    
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
