package fi.hel.integration.ya.starttiraha.models.tulorekisteri;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Payer {
    @JacksonXmlElementWrapper(localName = "PayerIds")
    @JacksonXmlProperty(localName = "Id")
    private List<Id> payerIds;

    @JacksonXmlElementWrapper(localName = "SubOrgs")
    @JacksonXmlProperty(localName = "SubOrg")
    private List<Id> subOrgs;

    // Getters and Setters
    public List<Id> getPayerIds() {
        return payerIds;
    }

    public void setPayerIds(List<Id> payerIds) {
        this.payerIds = payerIds;
    }

    public List<Id> getSubOrgs() {
        return subOrgs;
    }

    public void setSubOrgs(List<Id> subOrgs) {
        this.subOrgs = subOrgs;
    }
}
