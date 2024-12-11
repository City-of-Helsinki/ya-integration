package fi.hel.integration.ya.maksuliikenne.models.pain.groupHeader;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import fi.hel.integration.ya.maksuliikenne.models.pain.Id;

public class GroupHeader {
    @JacksonXmlProperty(localName = "MsgId")
    private String msgId;

    @JacksonXmlProperty(localName = "CreDtTm")
    private String creDtTm;

    @JacksonXmlProperty(localName = "NbOfTxs")
    private int nbOfTxs;

    @JacksonXmlProperty(localName = "CtrlSum")
    private BigDecimal ctrlSum;

    @JacksonXmlProperty(localName = "InitgPty")
    private InitiatingParty initgPty;

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }

    public String getCreDtTm() {
        return creDtTm;
    }

    public void setCreDtTm(String creDtTm) {
        this.creDtTm = creDtTm;
    }

    public int getNbOfTxs() {
        return nbOfTxs;
    }

    public void setNbOfTxs(int nbOfTxs) {
        this.nbOfTxs = nbOfTxs;
    }

    public BigDecimal getCtrlSum() {
        return ctrlSum;
    }

    public void setCtrlSum(BigDecimal ctrlSum) {
        this.ctrlSum = ctrlSum;
    }

    public InitiatingParty getInitgPty() {
        return initgPty;
    }

    public void setInitgPty(InitiatingParty initgPty) {
        this.initgPty = initgPty;
    }
}

