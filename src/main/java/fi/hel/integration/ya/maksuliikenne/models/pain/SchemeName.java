package fi.hel.integration.ya.maksuliikenne.models.pain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class SchemeName {
    @JacksonXmlProperty(localName = "Cd")
    private String cd;

    public String getCd() {
        return cd;
    }

    public void setCd(String cd) {
        this.cd = cd;
    }
}
