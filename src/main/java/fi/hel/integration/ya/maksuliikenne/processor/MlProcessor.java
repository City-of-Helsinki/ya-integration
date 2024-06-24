package fi.hel.integration.ya.maksuliikenne.processor;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import fi.hel.integration.ya.Utils;
import fi.hel.integration.ya.maksuliikenne.models.pain.MessageRoot;
import fi.hel.integration.ya.maksuliikenne.models.pain.Id;
import fi.hel.integration.ya.maksuliikenne.models.pain.OrgId;
import fi.hel.integration.ya.maksuliikenne.models.pain.Other;
import fi.hel.integration.ya.maksuliikenne.models.pain.SchemeName;
import fi.hel.integration.ya.maksuliikenne.models.pain.Document;
import fi.hel.integration.ya.maksuliikenne.models.pain.groupHeader.GroupHeader;
import fi.hel.integration.ya.maksuliikenne.models.pain.groupHeader.InitiatingParty;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.AccountIdentification;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.Debtor;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.DebtorAccount;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.DebtorAgent;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.FinInstnId;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.PaymentInfo;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.PostalAddress;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo.Amount;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo.CreditTransferTransaction;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo.Creditor;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo.CreditorAccount;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo.InstructedAmount;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo.PaymentId;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo.RemittanceInfo;
//import fi.hel.integration.ya.maksuliikenne.models.pain.groupHeader.InitgPty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@ApplicationScoped
@Named ("mlProcessor")
public class MlProcessor {
    
    @Inject
    Logger log;

    @Inject
    Utils utils;

    @ConfigProperty(name = "maksuliikenne_maksaja", defaultValue= "maksaja")
    String maksaja;

    @ConfigProperty(name = "maksuliikenne_maksupalvelutunnus", defaultValue = "maksupalvelutunnus")
    String maksupalvelutunnus;

    @ConfigProperty(name = "maksuliikenne_maksupalvelutunnus_code", defaultValue = "code")
    String code;

    @ConfigProperty(name = "maksuliikenne_maksaja_maakoodi", defaultValue = "FI")
    String maakoodi;

    @ConfigProperty(name = "maksuliikenne_maksaja_iban", defaultValue = "iban")
    String iban;

    @ConfigProperty(name = "maksuliikenne_maksaja_bic", defaultValue = "bic")
    String bic;

    private static final String ORIGINAL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String MSG_ID_PREFIX = "YA-";
    private static final String CHARGE_BEARER= "SLEV";
    private static final String PAYMENT_METHOD= "TRF";
    private static final boolean BATCH_BOOKING= true;
    private static final String DOCUMENT_XSI = "http://www.w3.org/2001/XMLSchema-instance";
    private static final String DOCUMENT_XSD = "http://www.w3.org/2001/XMLSchema";
    private static final String SCHEMA_LOCATION = "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03 pain.001.001.03.xsd";
    private static final String DOCUMENT_XMLNS = "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03";

    @SuppressWarnings("unchecked")
    public void mapPaymentTransactions(Exchange ex) {
        try {

            // All payments for the current day
            List<Map<String,Object>> body = ex.getIn().getBody(List.class);
            System.out.println("Start mapping the body :: " + body);
            
            String msgId = MSG_ID_PREFIX + utils.getCurrentTime("yyyy-MM-dd'T'HH:mm:ss");
            
            // Creation time
            String creDtTm = utils.getCurrentTime("yyyy-MM-dd'T'HH:mm:ssXXX");
            
            // total amounts of the payments and total sum of the payments
            Map<String,Object> totalAmounts = calculateTotalAmounts(body);
            int nbOfTxs = (int) totalAmounts.get("numberOfPmts");
            BigDecimal ctrlSum = (BigDecimal) totalAmounts.get("totalSumOfPmts");
            
            // The due date should be the same for all payments being processed together
            String duedate = (String) body.get(0).get("dueDate");
            Map<String,Object> delivery = (Map<String, Object>) body.get(0).get("delivery");
            String fileName = (String) delivery.get("fileName");
            fileName = fileName.substring(0, 10);

            Document document = new Document();
            MessageRoot messageRoot = new MessageRoot();
            GroupHeader  groupHeader = new GroupHeader();
            InitiatingParty initiatingParty = new InitiatingParty();
            Id initgPtyId = new Id();
            OrgId initgPtyOrgId = new OrgId();
            Other initgPtyOthr = new Other();
            PaymentInfo pmtInf = new PaymentInfo();
            SchemeName initPtyschmeNm = new SchemeName();
            Debtor debtor= new Debtor();
            SchemeName pmtInfSchmeNm = new SchemeName();
            Id pmtInfId = new Id();
            OrgId pmtInfOrgId = new OrgId();
            Other pmtInfOthr = new Other();
            PostalAddress postalAddress = new PostalAddress();
            DebtorAccount debtorAccount = new DebtorAccount();
            AccountIdentification accountIdentification= new AccountIdentification();
            DebtorAgent debtorAgent = new DebtorAgent();
            FinInstnId finInstnId = new FinInstnId();

            // Initiating party (Group header)
            initPtyschmeNm.setCd(code);
            initgPtyOthr.setSchmeNm(initPtyschmeNm);
            initgPtyOthr.setId(maksupalvelutunnus);
            initgPtyOrgId.setOther(initgPtyOthr);
            initgPtyId.setOrgId(initgPtyOrgId);
            initiatingParty.setNm(maksaja);
            initiatingParty.setId(initgPtyId);

            // Debtor (Payment info)
            postalAddress.setCtry(maakoodi);
            pmtInfSchmeNm.setCd(code);
            pmtInfOthr.setId(maksupalvelutunnus);
            pmtInfOthr.setSchmeNm(pmtInfSchmeNm);
            pmtInfOrgId.setOther(pmtInfOthr);
            pmtInfId.setOrgId(pmtInfOrgId);
            debtor.setId(pmtInfId);
            debtor.setNm(maksaja);
            debtor.setPstlAdr(postalAddress);
            
            // Debtor account (Payment info)
            accountIdentification.setIban(iban);
            debtorAccount.setId(accountIdentification);

            // Debtor agent (Payment info)
            finInstnId.setBic(bic);
            debtorAgent.setFinInstnId(finInstnId);
            
            // Credit transfer transaction information (Payment Info)
            List<CreditTransferTransaction> creditTransactionList = createListOfCreditTransactions(body);

            // GroupHeader
            groupHeader.setMsgId(msgId);
            groupHeader.setCreDtTm(creDtTm);
            groupHeader.setNbOfTxs(nbOfTxs);
            groupHeader.setCtrlSum(ctrlSum);
            groupHeader.setInitgPty(initiatingParty);

            // Payment Info
            pmtInf.setDbtrAgt(debtorAgent);
            pmtInf.setChrgBr(CHARGE_BEARER);
            pmtInf.setPmtInfId(utils.convertDate(duedate, ORIGINAL_DATE_FORMAT, "yyyyMMdd") + "-" + fileName);
            pmtInf.setPmtMtd(PAYMENT_METHOD);
            pmtInf.setBtchBookg(BATCH_BOOKING);
            pmtInf.setReqdExctnDt(utils.convertDate(duedate, ORIGINAL_DATE_FORMAT, "yyyy-MM-dd"));
            pmtInf.setDbtr(debtor);
            pmtInf.setDbtrAcct(debtorAccount);
            pmtInf.setCdtTrfTxInfList(creditTransactionList);

            // Message root
            messageRoot.setGroupHeader(groupHeader);
            messageRoot.setPaymentInformation(pmtInf);
            
            document.setXsi(DOCUMENT_XSI);
            document.setXsd(DOCUMENT_XSD);
            document.setSchemaLocation(SCHEMA_LOCATION);
            document.setXmlns(DOCUMENT_XMLNS);
            document.setMessageRoot(messageRoot);

            ex.getIn().setBody(document);

        } catch (Exception e){
            log.error(e);
            e.printStackTrace();
            ex.setException(e);
        }
    }

    /**
     * Creates a list of {@code CreditTransferTransaction} objects from the provided list of payment details.
     * 
     * @param body a list of payments
     * @return a list of {@code CreditTransferTransaction} objects constructed from the provided payment details
     */
    @SuppressWarnings("unchecked")
    private List<CreditTransferTransaction> createListOfCreditTransactions(List<Map<String,Object>> body) {

        List<CreditTransferTransaction> creditTransactions = new ArrayList<>();
        
        for(Map<String,Object> payment: body) {
            
            Map<String,Object> delivery = (Map<String, Object>) payment.get("delivery");
            int id = (int) delivery.get("id");
            
            Map<String,Object> receiver = (Map<String, Object>) payment.get("receiver");
            Map<String,Object> bankAccount = (Map<String, Object>) receiver.get("bankAccount");
            String iban = (String) bankAccount.get("value");
            
            Map<String,Object> name = (Map<String, Object>) receiver.get("name");
            String lastName = (String) name.get("lastName");
            String firstName = (String) name.get("firstName");
            String receiverName = "";
            if(lastName.equals("")) {
                receiverName = firstName;

            } else {
                receiverName = lastName + ", " + firstName;
            }

            CreditTransferTransaction creditTransferTransaction = new CreditTransferTransaction();
            PaymentId pmtId = new PaymentId();
            Amount amt = new Amount();
            InstructedAmount instructedAmount = new InstructedAmount();
            Creditor creditor = new Creditor();
            CreditorAccount creditorAccount = new CreditorAccount();
            AccountIdentification accountIdentification = new AccountIdentification();
            RemittanceInfo remittanceInfo = new RemittanceInfo();

            // Payment id (invoice number)
            pmtId.setEndToEndId(id);
    
            // Amount
            instructedAmount.setCcy((String) payment.get("currency"));
            instructedAmount.setValue((double) payment.get("grossSum"));
            amt.setInstdAmt(instructedAmount);
           
            // Creditor
            creditor.setNm(receiverName);
            
            // Creditor account
            accountIdentification.setIban(iban);
            creditorAccount.setId(accountIdentification);

            // Remittance information
            remittanceInfo.setUstrd((String)payment.get("ourReference"));

            creditTransferTransaction.setPmtId(pmtId);
            creditTransferTransaction.setAmt(amt);
            creditTransferTransaction.setCdtr(creditor);
            creditTransferTransaction.setCdtrAcct(creditorAccount);
            creditTransferTransaction.setRmtInf(remittanceInfo);

            creditTransactions.add(creditTransferTransaction);
        }

        return creditTransactions;

    }

    /**
     * Calculates the total number and sum of payments from the provided list of payments
     *
     * @param body a list of maps where each map contains details of a payment
     * @return a map containing two entries:
     *         - {@code numberOfPmts}: an {@code int} representing the total number of payments</li>
     *         - {@code totalSumOfPmts}: a {@code BigDecimal} representing the total sum of all payments</li>
     */
    private Map<String,Object> calculateTotalAmounts(List<Map<String,Object>> body) {
        Map<String,Object> totalAmounts = new LinkedHashMap<>();
        int numberOfPmts = 0;
        BigDecimal totalSumOfPmts = new BigDecimal(0);

        for(Map<String,Object> payment: body) {

            double grossSum = (double) payment.get("grossSum");
            BigDecimal paymentSum = BigDecimal.valueOf(grossSum);
            totalSumOfPmts = totalSumOfPmts.add(paymentSum);
            numberOfPmts+=1;
        }

        totalAmounts.put("numberOfPmts", numberOfPmts);
        totalAmounts.put("totalSumOfPmts", totalSumOfPmts);

        return totalAmounts;
    }
}
