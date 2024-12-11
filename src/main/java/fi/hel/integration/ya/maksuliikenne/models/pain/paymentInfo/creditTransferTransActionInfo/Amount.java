package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Amount {
    @JacksonXmlProperty(localName = "InstdAmt")
    private InstructedAmount instdAmt;

    public InstructedAmount getInstdAmt() {
        return instdAmt;
    }

    public void setInstdAmt(InstructedAmount instdAmt) {
        this.instdAmt = instdAmt;
    }
}
