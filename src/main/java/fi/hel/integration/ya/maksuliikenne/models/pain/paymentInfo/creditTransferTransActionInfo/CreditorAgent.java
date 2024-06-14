package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.FinInstnId;

public class CreditorAgent {
    @JacksonXmlProperty(localName = "FinInstnId")
    private FinInstnId finInstnId;

    public FinInstnId getFinInstnId() {
        return finInstnId;
    }

    public void setFinInstnId(FinInstnId finInstnId) {
        this.finInstnId = finInstnId;
    }
}
