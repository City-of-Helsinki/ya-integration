package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class CreditTransferTransaction {
    @JacksonXmlProperty(localName = "PmtId")
    private PaymentId pmtId;

    @JacksonXmlProperty(localName = "Amt")
    private Amount amt;

    //@JacksonXmlProperty(localName = "CdtrAgt")
    //private CreditorAgent cdtrAgt;

    @JacksonXmlProperty(localName = "Cdtr")
    private Creditor cdtr;

    @JacksonXmlProperty(localName = "CdtrAcct")
    private CreditorAccount cdtrAcct;

    @JacksonXmlProperty(localName = "RmtInf")
    private RemittanceInfo rmtInf;

    public PaymentId getPmtId() {
        return pmtId;
    }

    public void setPmtId(PaymentId pmtId) {
        this.pmtId = pmtId;
    }

    public Amount getAmt() {
        return amt;
    }

    public void setAmt(Amount amt) {
        this.amt = amt;
    }

    //public CreditorAgent getCdtrAgt() {
    //    return cdtrAgt;
    //}

    //public void setCdtrAgt(CreditorAgent cdtrAgt) {
    //    this.cdtrAgt = cdtrAgt;
    //}

    public Creditor getCdtr() {
        return cdtr;
    }

    public void setCdtr(Creditor cdtr) {
        this.cdtr = cdtr;
    }

    public CreditorAccount getCdtrAcct() {
        return cdtrAcct;
    }

    public void setCdtrAcct(CreditorAccount cdtrAcct) {
        this.cdtrAcct = cdtrAcct;
    }

    public RemittanceInfo getRmtInf() {
        return rmtInf;
    }

    public void setRmtInf(RemittanceInfo rmtInf) {
        this.rmtInf = rmtInf;
    }
}
