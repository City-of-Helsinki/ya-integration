package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Creditor {
    @JacksonXmlProperty(localName = "Nm")
    private String nm;

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
