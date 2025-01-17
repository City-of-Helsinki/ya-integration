package fi.hel.integration.ya.starttiraha.processor;


import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.camel.Exchange;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import fi.hel.integration.ya.Utils;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.BenefitReportsRequestToIR;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.ContactPerson;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.DeliveryData;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.DeliveryDataCreator;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.DeliveryDataOwner;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.DeliveryDataSender;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.EarningPeriod;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.Id;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.IncomeEarner;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.Payer;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.Report;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.ReportData;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.PaymentTransaction;
import fi.hel.integration.ya.starttiraha.models.tulorekisteri.TransactionBasic;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named ("tulorekisteriProcessor")
public class TulorekisteriProcessor {

    @Inject
    Logger log;

    @Inject
    Utils utils;

    @ConfigProperty(name = "TULOREKISTERI_CONTACTPERSON", defaultValue = "contactPerson")
    String contactPerson;

    @ConfigProperty(name = "TULOREKISTERI_CONTACTPHONE", defaultValue = "phone" )
    String phoneNumber;

    @ConfigProperty(name = "TULOREKISTERI_CONTACTEMAIL", defaultValue = "email")
    String email;

    @ConfigProperty(name = "TULOREKISTERI_TRANSACTIONCODE", defaultValue = "code")
    String transactionCode;

    @ConfigProperty(name = "TULOREKISTERI_TRANSACTIONTAXCODE", defaultValue = "codeTax")
    String transactionCodeTax;

    @ConfigProperty(name = "TULOREKISTERI_DELIVERYSOURCE", defaultValue = "source")
    String deliverySource;

    @ConfigProperty(name = "TULOREKISTERI_SUBORGCODE", defaultValue = "subOrg")
    String subOrgCode;

    private final int DELIVERY_DATATYPE = 102;
    private final int FAULTY_CONTROL = 1;
    private final boolean PRODUCTION_ENVIRONMENT=true;
    private final int DELIVERY_DATA_OWNER_TYPE = 1;
    private final int DELIVERY_DATA_CREATOR_TYPE = 1;
    private final int DELIVERY_DATA_SENDER_TYPE = 1;
    private final int PAYER_ID_TYPE = 1;
    private final int SUBORG_TYPE = 2;
    private final int REPORT_DATA_ACTION_CODE = 1;
    private final int INCOME_EARNER_ID_TYPE = 2;
    private final String ORIGINAL_DATE_FORMAT = "d.M.yyyy";
    private final String DESIRED_DATE_FORMAT = "yyyy-MM-dd";
    private final int RESPONSIBILITY_CODE1 = 1;
    private final int RESPONSIBILITY_CODE2 = 2;
    private final String TRANSACTIONCODEGARNISHMENT = "1269";

    private final String XMLNS_BRTIR = "http://www.tulorekisteri.fi/2017/1/BenefitReportsToIR";
    private final String XSI = "http://www.w3.org/2001/XMLSchema-instance";
    private final String SCHEMA_LOCATION = "http://www.tulorekisteri.fi/2017/1/BenefitReportsToIR BenefitReportsToIR.xsd";

    @SuppressWarnings("unchecked")
    public void mapIncomeRegisterData (Exchange ex) {
    
        try {

            List<Map<String, Object>> body = ex.getIn().getBody(List.class);
        
            String timeStamp = utils.getCurrentTime("yyyy-MM-dd'T'HH:mm:ssXXX");
            String paymentDate = (String) body.get(0).get("paymentDate");
            paymentDate = utils.convertDate(paymentDate, ORIGINAL_DATE_FORMAT, DESIRED_DATE_FORMAT);
            String payerId = (String) body.get(0).get("payerId");
            if (payerId != null) {  
               payerId = payerId.replace("\uFEFF", ""); // Remove BOM character if present
            }
            String deliveryId = utils.getCurrentTime("yyyy-MM-dd'T'HH-mm-ss");

            BenefitReportsRequestToIR benefitReportsRequestToIR = new BenefitReportsRequestToIR();
            DeliveryData deliveryData = new DeliveryData();
            DeliveryDataOwner deliveryDataOwner = new DeliveryDataOwner();
            DeliveryDataCreator deliveryDataCreator = new DeliveryDataCreator();
            DeliveryDataSender deliveryDataSender = new DeliveryDataSender();
            ContactPerson contactPerson1 = new ContactPerson();
            ContactPerson contactPerson2 = new ContactPerson();
            Payer payer = new Payer();
            Id id = new Id();
            Id subOrg = new Id();
            

            deliveryData.setTimestamp(timeStamp);
            deliveryData.setSource(deliverySource);
            deliveryData.setDeliveryDataType(DELIVERY_DATATYPE );
            deliveryData.setDeliveryId(deliveryId);
            deliveryData.setFaultyControl(FAULTY_CONTROL);
            deliveryData.setProductionEnvironment(PRODUCTION_ENVIRONMENT);
            deliveryDataOwner.setType(DELIVERY_DATA_OWNER_TYPE);
            deliveryDataOwner.setCode(payerId);
            deliveryDataCreator.setType(DELIVERY_DATA_CREATOR_TYPE);
            deliveryDataCreator.setCode(payerId);
            deliveryDataSender.setType(DELIVERY_DATA_SENDER_TYPE);
            deliveryDataSender.setCode(payerId);
            deliveryData.setPaymentDate(paymentDate);
            deliveryData.setDeliveryDataOwner(deliveryDataOwner);
            deliveryData.setDeliveryDataCreator(deliveryDataCreator);
            deliveryData.setDeliveryDataSender(deliveryDataSender);

            List<ContactPerson> contactPersons = new ArrayList<>();
            contactPerson1.setName(contactPerson);
            contactPerson1.setTelephone(phoneNumber);
            contactPerson1.setEmail(email);
            contactPerson1.setResponsibilityCode(RESPONSIBILITY_CODE1);
            contactPersons.add(contactPerson1);
            contactPerson2.setName(contactPerson);
            contactPerson2.setTelephone(phoneNumber);
            contactPerson2.setEmail(email);
            contactPerson2.setResponsibilityCode(RESPONSIBILITY_CODE2);
            contactPersons.add(contactPerson2);
            deliveryData.setContactPersons(contactPersons);

            List<Id> payerIds = new ArrayList<>();

            id.setType(PAYER_ID_TYPE);
            id.setCode(payerId);
            payerIds.add(id);
            payer.setPayerIds(payerIds);

            List<Id> subOrgs = new ArrayList<>();
            subOrg.setType(SUBORG_TYPE);
            subOrg.setCode(subOrgCode);
            subOrgs.add(subOrg);
            payer.setSubOrgs(subOrgs);

            deliveryData.setPayer(payer);
          
            List<Report> reports = createListOfReports(body);
            List<PaymentTransaction> transactions = new ArrayList<>();
            
            deliveryData.setReports(reports);
            benefitReportsRequestToIR.setBrtir(XMLNS_BRTIR);
            benefitReportsRequestToIR.setXsi(XSI);
            benefitReportsRequestToIR.setSchemaLocation(SCHEMA_LOCATION);
            benefitReportsRequestToIR.setDeliveryData(deliveryData);
            ex.getIn().setBody(benefitReportsRequestToIR);


        } catch (Exception e){
            log.error(e);
            e.printStackTrace();
            ex.setException(e);
        }
        
    }

    private List<Report> createListOfReports(List<Map<String, Object>> body) {
        List<Report> reports = new ArrayList<>();
        
        for(Map<String, Object> payment: body) {

            List<PaymentTransaction> transactions = new ArrayList<>();

            Report report = new Report();
            ReportData reportData = new ReportData();
            IncomeEarner incomeEarner = new IncomeEarner();
            Id incomeEarnerId = new Id();
            PaymentTransaction transaction = new PaymentTransaction();
            PaymentTransaction transactionTax = new PaymentTransaction();
            PaymentTransaction transactionGarnishment = new PaymentTransaction();
            TransactionBasic transactionBasic = new TransactionBasic();
            TransactionBasic transactionBasicTax = new TransactionBasic();
            TransactionBasic transactionBasicGarnishment = new TransactionBasic();
            EarningPeriod earningPeriod = new EarningPeriod();
            EarningPeriod earningPeriodTax = new EarningPeriod();
            EarningPeriod earningPeriodGarnishment = new EarningPeriod();

            reportData.setActionCode(REPORT_DATA_ACTION_CODE);
            String decisionNumber = (String) payment.get("decisionNumber");
            decisionNumber = decisionNumber.replaceAll("/", "");
            String date = (String) payment.get("startDate");
            date = utils.convertDate(date, ORIGINAL_DATE_FORMAT, "ddMMyyyy");
            String reportId = decisionNumber + date;
            reportData.setReportId(reportId);
            

            List<Id> incomeEarnerIds = new ArrayList<>();
            incomeEarnerId.setType(INCOME_EARNER_ID_TYPE);
            incomeEarnerId.setCode((String) payment.get("hetu"));
            incomeEarnerIds.add(incomeEarnerId);
            incomeEarner.setIncomeEarnerIds(incomeEarnerIds);
            

            transactionBasic.setTransactionCode(transactionCode);
            String amount = (String) payment.get("amount");
            amount = amount.replaceAll(",", ".");
            transactionBasic.setAmount(amount);
            transaction.setTransactionBasic(transactionBasic);
            String startDate = (String) payment.get("startDate");
            startDate = utils.convertDate(startDate, ORIGINAL_DATE_FORMAT, DESIRED_DATE_FORMAT);
            String endDate = (String) payment.get("endDate");
            endDate = utils.convertDate(endDate, ORIGINAL_DATE_FORMAT, DESIRED_DATE_FORMAT);
            earningPeriod.setStartDate(startDate);
            earningPeriod.setEndDate(endDate);
            transaction.setEarningPeriod(earningPeriod);
            transactions.add(transaction);

            transactionBasicTax.setTransactionCode(transactionCodeTax);
            String taxAmount = (String) payment.get("taxAmount");
            taxAmount = taxAmount.replaceAll(",", ".");
            transactionBasicTax.setAmount(taxAmount);
            transactionTax.setTransactionBasic(transactionBasicTax);
            String paymentDateTax = (String) payment.get("paymentDate2");
            paymentDateTax = utils.convertDate(paymentDateTax, ORIGINAL_DATE_FORMAT, DESIRED_DATE_FORMAT);
            earningPeriodTax.setStartDate(paymentDateTax);
            earningPeriodTax.setEndDate(paymentDateTax);
            transactionTax.setEarningPeriod(earningPeriodTax);
            transactions.add(transactionTax);

            String garnishmentAmount = (String) payment.get("garnishmentAmount");

            if(!garnishmentAmount.equals("0,00") && !garnishmentAmount.equals("0")) {
                transactionBasicGarnishment.setTransactionCode(TRANSACTIONCODEGARNISHMENT);
                garnishmentAmount = garnishmentAmount.replaceAll(",", ".");
                transactionBasicGarnishment.setAmount(garnishmentAmount);
                transactionGarnishment.setTransactionBasic(transactionBasicGarnishment);
                String paymentDate = (String) payment.get("paymentDate2");
                paymentDate = utils.convertDate(paymentDate, ORIGINAL_DATE_FORMAT, DESIRED_DATE_FORMAT);
                earningPeriodGarnishment.setStartDate(paymentDate);
                earningPeriodGarnishment.setEndDate(paymentDate);
                transactionGarnishment.setEarningPeriod(earningPeriodGarnishment);
                transactions.add(transactionGarnishment);
            }

            report.setIncomeEarner(incomeEarner);
            report.setReportData(reportData);
            report.setTransactions(transactions);
            reports.add(report);
        }

        return reports;
    }

    public void fetchFileFromSftp(Exchange ex) {
        Session session = null;
        ChannelSftp channelSftp = null;

        try {
            String hostname = ex.getIn().getHeader("hostname", String.class);
            String username = ex.getIn().getHeader("username", String.class);
            String privateKeyEncoded = ex.getIn().getHeader("privateKey", String.class);
            String directoryPath = ex.getIn().getHeader("directoryPath", String.class);

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

            Vector<ChannelSftp.LsEntry> fileList = channelSftp.ls(directoryPath);
            List<ChannelSftp.LsEntry> filesOnly = fileList.stream()
                .filter(entry -> !entry.getAttrs().isDir()) 
                .toList();

            if (filesOnly == null || filesOnly.isEmpty()) {
                log.infof("No files found in the directory: %s", directoryPath);
                ex.getIn().setBody(""); 
                ex.getIn().setHeader(Exchange.FILE_NAME, null);
                ex.getIn().setHeader("CamelFtpReplyCode", "204"); // 204 - No Content
                ex.getIn().setHeader("CamelFtpReplyString", "No files found");
                return;
            }

            if (filesOnly.size() != 1) {
                throw new IllegalStateException("Expected exactly one file, but found: " + filesOnly.size());
            }

            ChannelSftp.LsEntry fileEntry = filesOnly.get(0);
            String fileName = fileEntry.getFilename();
            String remoteFilePath = directoryPath + "/" + fileName;

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channelSftp.get(remoteFilePath, outputStream);
            String fileContent = outputStream.toString("UTF-8");

            ex.getIn().setBody(fileContent);
            ex.getIn().setHeader(Exchange.FILE_NAME, fileName);

            ex.getIn().setHeader("CamelFtpReplyCode", "200");
            ex.getIn().setHeader("CamelFtpReplyString", "File fetched successfully");

            log.infof("File '%s' fetched successfully from directory: %s", fileName, directoryPath);

        } catch (Exception e) {
            log.error("Error during SFTP fetch: {}", e.getMessage(), e);
            ex.setException(e);
            ex.getIn().setHeader("CamelFtpReplyCode", "500");
            ex.getIn().setHeader("CamelFtpReplyString", "Error during SFTP fetch");

        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    public void removeFileFromSftp(Exchange ex) {
        Session session = null;
        ChannelSftp channelSftp = null;
    
        try {

            String hostname = ex.getIn().getHeader("hostname", String.class);
            String username = ex.getIn().getHeader("username", String.class);
            String privateKeyEncoded = ex.getIn().getHeader("privateKey", String.class);
            String filePath = ex.getIn().getHeader("filePathToRemove", String.class);

            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyEncoded);
    
            JSch jsch = new JSch();
            jsch.addIdentity("privateKey", privateKeyBytes, null, null);
    
            session = jsch.getSession(username, hostname, 22);
    
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
    
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
    
            channelSftp.rm(filePath);
    
            log.infof("File '%s' removed successfully from SFTP server", filePath);
    
        } catch (Exception e) {
            log.error("Error during SFTP file deletion: {}", e.getMessage(), e);
            ex.setException(e);
    
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
