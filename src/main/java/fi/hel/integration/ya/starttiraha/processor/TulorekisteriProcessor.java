package fi.hel.integration.ya.starttiraha.processor;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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
    private final boolean PRODUCTION_ENVIRONMENT = true;
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
            String deliveryId = utils.getCurrentTime("yyyy-MM-dd'T'HH:mm:ss");

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
            System.out.println("Raw payerId :: [" + deliveryDataOwner.getCode() + "]");
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
            TransactionBasic transactionBasic = new TransactionBasic();
            TransactionBasic transactionBasicTax = new TransactionBasic();
            EarningPeriod earningPeriod = new EarningPeriod();
            EarningPeriod earningPeriodTax = new EarningPeriod();

            reportData.setActionCode(REPORT_DATA_ACTION_CODE);
            reportData.setReportId((String) payment.get("decisionNumber"));
            

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

            report.setIncomeEarner(incomeEarner);
            report.setReportData(reportData);
            report.setTransactions(transactions);
            reports.add(report);
        }

        return reports;
    }
}