package fi.hel.integration.ya.maksuliikenne.processor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_COMPANYCODE", defaultValue= "companyCode")
    String companyCode;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_DOCUMENTTYPE", defaultValue= "documentType")
    String documentType;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_TULOSYKSIKKO", defaultValue= "profitCenter")
    String profitCenter;

    @ConfigProperty(name = "MAKSULIIKENNE_KIRJANPITO_SENDERID", defaultValue= "senderId")
    String senderId;

    private static final String EMPTY = "";
    
    // Claim types:
    private static final String PT = "PALKKATUKI";
    private static final String PT55 = "PALKKATUKI_55V";
    private static final String HKK = "HARKINNANVARAINEN_KULUKORVAUS";
    private static final String MYK = "MATKA_JA_YOPYMISKUSTANNUSTENKORVAUS";
    private static final String TOJT = "TYOOLOSUHTEIDEN_JARJESTELYTUKI";

    @SuppressWarnings("unchecked")
    public void mapAccountigData(Exchange ex) {
        
        try {

            SBO_SimpleAccountingContainer simpleAccountingcontainer = new SBO_SimpleAccountingContainer();
            SBO_SimpleAccounting simpleAccounting = new SBO_SimpleAccounting();
            LineItemType lineItemType = new LineItemType();
            List<LineItemType> lineItemTypes = new ArrayList<>();
            List<SBO_SimpleAccounting> sbo_SimpleAccountings = new ArrayList<>();
            
            Map<String,Object> body = ex.getIn().getBody(Map.class);
            //System.out.println("Accounting data map :: " + body);
            String claimType = (String) body.get("claimType");
            simpleAccounting.setSenderId(senderId);
            simpleAccounting.setCompanyCode(companyCode);
            simpleAccounting.setDocumentType(documentType);

            String date = utils.getCurrentTime("YYYYMMdd");
            System.out.println("date :: " + date);
            simpleAccounting.setDocumentDate(date);
            simpleAccounting.setPostingDate(date);
            
            // oltava numeerinen arvo ja 9 merkkiä, tarvittaessa täytetään etunollilla
            Map<String, Object> delivery = (Map<String, Object>) body.get("delivery");
            int id = (int) delivery.get("id");
            // Convert the integer ID to a string with leading zeros to ensure it is 9 characters long
            String formattedId = String.format("%09d", id);
            simpleAccounting.setReference(formattedId);

            simpleAccounting.setHeaderText(claimType);

            String currency = (String) body.get("currency");
            simpleAccounting.setCurrencyCode(currency);

            Map<String, Object> posting = (Map<String, Object>) body.get("posting");
            List<Map<String, Object>> postingInstallment = (List<Map<String, Object>>) posting.get("postingInstallment");
            String vatCode = (String) postingInstallment.get(0).get("vatCode");
            
            if(vatCode.length() >= 2) {
                vatCode = vatCode.substring(vatCode.length() - 2);
            }

            lineItemType.setTaxCode(vatCode);

            double sum = (double) body.get("grossSum");
            System.out.println("sum :: " + sum);
            String sumAsString = String.valueOf(sum);
            
            sumAsString = sumAsString.replaceAll("\\.", ",");
            System.out.println("sumAsString :: " + sumAsString);
            lineItemType.setAmountInDocumentCurrency(sumAsString);

            String ourRefence = (String) body.get("ourReference");
            String cutOurReference = ourRefence.length() > 50 ? ourRefence.substring(0, 50) : ourRefence;

            lineItemType.setLineText(cutOurReference);

            // TODO: hae kumppanikoodilistaus verkkoasemalta
            /* String filePath = "src/main/resources/kumppanikoodilistaus 16.8.2024.xlsx";
            Map<String, Object> receiver = (Map<String, Object>) body.get("receiver");
            String businessId = (String) receiver.get("businessId");
            //System.out.println("BUSINESS ID :: " + businessId);
            businessId = "0668319-4";
            String partnerCode = getPartnerCode(businessId, filePath);
            System.out.println("partnercode :: " + partnerCode);

            if (partnerCode == null || partnerCode.isEmpty()) {
                lineItemType.setTradingPartner(EMPTY);
            } else {
                lineItemType.setTradingPartner(partnerCode);
            } */

            lineItemType.setTradingPartner(EMPTY);

            // kirjaustunniste (sisäinen tilaus), pituus 10 numeroa, pakollinen
            // mäpätään claimTypen perusteella
            String orderItemNumber = getOrderItemNumber(claimType);
            lineItemType.setOrderItemNumber(orderItemNumber);
            
            // SAP pääkirjatili (6 numeroa) -> pääkirjatili (tulos), pakollinen
            // mäpätään claimTypen perusteella
            String glAccount = getGlAccount(claimType);
            lineItemType.setGlAccount(glAccount);
            
            // SAP- tulosyksikkö (7 numeroa), pakollinen
            lineItemType.setProfitCenter(profitCenter);

            lineItemTypes.add(lineItemType);
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
        //String filePath = "src/main/resources/kumppanikoodilistaus 16.8.2024.xlsx";
        
        Map<String, String> kumppanikoodit = readExcelFile(filePath);

        String partnerCode = kumppanikoodit.get(businessId);
        System.out.println("Partner code :: " + partnerCode);
        return partnerCode;

    }

    private static Map<String, String> readExcelFile(String filePath) {
        Map<String, String> kumppanikoodit = new HashMap<>();

        try (Workbook workbook = new XSSFWorkbook(Files.newInputStream(Paths.get(filePath)))) {
            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                Cell ytunnusCell = row.getCell(1); // Y-tunnus is in the 2nd column
                Cell kumppanikoodiCell = row.getCell(2); // kumppanikoodi is in the 3rd column

                if (ytunnusCell != null && kumppanikoodiCell != null) {
                    String ytunnus = ytunnusCell.getStringCellValue().trim();
                    String kumppanikoodi = getCellValueAsString(kumppanikoodiCell);
                    kumppanikoodit.put(ytunnus, kumppanikoodi);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Kumppanikoodit :: " + kumppanikoodit);
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
}

