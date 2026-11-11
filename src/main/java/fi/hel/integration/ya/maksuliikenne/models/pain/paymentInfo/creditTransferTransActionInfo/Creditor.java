package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Creditor {
    @JacksonXmlProperty(localName = "Nm")
    private String nm;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JacksonXmlProperty(localName = "PstlAdr")
    private PostalAddressCreditor pstlAdr;

    public String getNm() {
        return nm;
    }

    public void setNm(String nm) {
        this.nm = nm;
    }

     public PostalAddressCreditor getPstlAdr() {
        return pstlAdr;
    }

    public void setPstlAdr(PostalAddressCreditor pstlAdr) {
        this.pstlAdr = pstlAdr;
    }
}
