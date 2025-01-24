package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class RemittanceInfo {
    @JacksonXmlProperty(localName = "Ustrd")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String ustrd;

    @JacksonXmlProperty(localName = "Strd")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private StructuredInfo strd;

    public String getUstrd() {
        return ustrd;
    }

    public void setUstrd(String ustrd) {
        this.ustrd = ustrd;
    }

    public StructuredInfo getStrd() {
        return strd;
    }

    public void setStrd(StructuredInfo strd) {
        this.strd = strd;
    }
}
