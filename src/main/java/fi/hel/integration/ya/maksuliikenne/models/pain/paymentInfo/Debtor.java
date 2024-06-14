package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import fi.hel.integration.ya.maksuliikenne.models.pain.Id;

public class Debtor {
    @JacksonXmlProperty(localName = "Nm")
    private String nm;

    @JacksonXmlProperty(localName = "PstlAdr")
    private PostalAddress pstlAdr;

    @JacksonXmlProperty(localName = "Id")
    private Id id;

    public String getNm() {
        return nm;
    }

    public void setNm(String nm) {
        this.nm = nm;
    }

    public PostalAddress getPstlAdr() {
        return pstlAdr;
    }

    public void setPstlAdr(PostalAddress pstlAdr) {
        this.pstlAdr = pstlAdr;
    }

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }
}
