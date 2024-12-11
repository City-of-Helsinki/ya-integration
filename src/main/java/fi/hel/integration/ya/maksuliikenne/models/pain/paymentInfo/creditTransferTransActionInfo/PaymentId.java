package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class PaymentId {
    @JacksonXmlProperty(localName = "EndToEndId")
    private int endToEndId;

    public int getEndToEndId() {
        return endToEndId;
    }

    public void setEndToEndId(int endToEndId) {
        this.endToEndId = endToEndId;
    }
}
