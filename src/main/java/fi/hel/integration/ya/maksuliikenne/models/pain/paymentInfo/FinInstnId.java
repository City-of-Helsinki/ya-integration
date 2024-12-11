package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class FinInstnId {
    @JacksonXmlProperty(localName = "BIC")
    private String bic;

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }
}
