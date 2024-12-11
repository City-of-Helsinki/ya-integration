package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class PostalAddress {
    @JacksonXmlProperty(localName = "Ctry")
    private String ctry;

    public String getCtry() {
        return ctry;
    }

    public void setCtry(String ctry) {
        this.ctry = ctry;
    }
}
