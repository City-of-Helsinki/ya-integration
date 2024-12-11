package fi.hel.integration.ya.maksuliikenne.models.pain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Other {
    @JacksonXmlProperty(localName = "Id")
    private String id;

    @JacksonXmlProperty(localName = "SchmeNm")
    private SchemeName schmeNm;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SchemeName getSchmeNm() {
        return schmeNm;
    }

    public void setSchmeNm(SchemeName schmeNm) {
        this.schmeNm = schmeNm;
    }
}
