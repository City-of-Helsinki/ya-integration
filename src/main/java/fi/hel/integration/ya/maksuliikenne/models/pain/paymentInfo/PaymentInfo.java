package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo.CreditTransferTransaction;

public class PaymentInfo {
    @JacksonXmlProperty(localName = "PmtInfId")
    private String pmtInfId;

    @JacksonXmlProperty(localName = "PmtMtd")
    private String pmtMtd;

    @JacksonXmlProperty(localName = "BtchBookg")
    private boolean btchBookg;

    @JacksonXmlProperty(localName = "ReqdExctnDt")
    private String reqdExctnDt;

    @JacksonXmlProperty(localName = "Dbtr")
    private Debtor dbtr;

    @JacksonXmlProperty(localName = "DbtrAcct")
    private DebtorAccount dbtrAcct;

    @JacksonXmlProperty(localName = "DbtrAgt")
    private DebtorAgent dbtrAgt;

    @JacksonXmlProperty(localName = "ChrgBr")
    private String chrgBr;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "CdtTrfTxInf")
    private List<CreditTransferTransaction> cdtTrfTxInfList;

    public String getPmtInfId() {
        return pmtInfId;
    }

    public void setPmtInfId(String pmtInfId) {
        this.pmtInfId = pmtInfId;
    }

    public String getPmtMtd() {
        return pmtMtd;
    }

    public void setPmtMtd(String pmtMtd) {
        this.pmtMtd = pmtMtd;
    }

    public boolean isBtchBookg() {
        return btchBookg;
    }

    public void setBtchBookg(boolean btchBookg) {
        this.btchBookg = btchBookg;
    }

    public String getReqdExctnDt() {
        return reqdExctnDt;
    }

    public void setReqdExctnDt(String reqdExctnDt) {
        this.reqdExctnDt = reqdExctnDt;
    }

    public Debtor getDbtr() {
        return dbtr;
    }

    public void setDbtr(Debtor dbtr) {
        this.dbtr = dbtr;
    }

    public DebtorAccount getDbtrAcct() {
        return dbtrAcct;
    }

    public void setDbtrAcct(DebtorAccount dbtrAcct) {
        this.dbtrAcct = dbtrAcct;
    }

    public DebtorAgent getDbtrAgt() {
        return dbtrAgt;
    }

    public void setDbtrAgt(DebtorAgent dbtrAgt) {
        this.dbtrAgt = dbtrAgt;
    }

    public String getChrgBr() {
        return chrgBr;
    }

    public void setChrgBr(String chrgBr) {
        this.chrgBr = chrgBr;
    }

    public List<CreditTransferTransaction> getCdtTrfTxInfList() {
        return cdtTrfTxInfList;
    }

    public void setCdtTrfTxInfList(List<CreditTransferTransaction> cdtTrfTxInfList) {
        this.cdtTrfTxInfList = cdtTrfTxInfList;
    }
}
