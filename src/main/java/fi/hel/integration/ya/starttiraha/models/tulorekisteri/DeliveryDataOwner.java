package fi.hel.integration.ya.starttiraha.models.tulorekisteri;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class DeliveryDataOwner {

    @JacksonXmlProperty(localName = "Type")
    private int type;

    @JacksonXmlProperty(localName = "Code")
    private String code;

    // Getters and Setters
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
    