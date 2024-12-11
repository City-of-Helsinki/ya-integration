package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class DebtorAgent {
    @JacksonXmlProperty(localName = "FinInstnId")
    private FinInstnId finInstnId;

    public FinInstnId getFinInstnId() {
        return finInstnId;
    }

    public void setFinInstnId(FinInstnId finInstnId) {
        this.finInstnId = finInstnId;
    }
}
