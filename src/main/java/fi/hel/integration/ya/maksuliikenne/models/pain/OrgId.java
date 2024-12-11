package fi.hel.integration.ya.maksuliikenne.models.pain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class OrgId {
    @JacksonXmlProperty(localName = "Othr")
    private Other other;

    public Other getOther() {
        return other;
    }

    public void setOther(Other other) {
        this.other = other;
    }
}
