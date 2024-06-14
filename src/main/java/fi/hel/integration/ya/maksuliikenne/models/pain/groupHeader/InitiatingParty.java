package fi.hel.integration.ya.maksuliikenne.models.pain.groupHeader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import fi.hel.integration.ya.maksuliikenne.models.pain.Id;

public class InitiatingParty {
    @JacksonXmlProperty(localName = "Nm")
    private String nm;

    @JacksonXmlProperty(localName = "Id")
    private Id id;

    public String getNm() {
        return nm;
    }

    public void setNm(String nm) {
        this.nm = nm;
    }

    public Id getId() {
        return id;
    }

    public void setId(Id id) {
        this.id = id;
    }
}
