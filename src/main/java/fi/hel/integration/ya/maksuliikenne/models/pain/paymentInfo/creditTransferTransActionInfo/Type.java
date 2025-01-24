package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Type {
    @JacksonXmlProperty(localName = "CdOrPrtry")
        private CodeOrProprietary cdOrPrtry;

        public CodeOrProprietary getCdOrPrtry() {
            return cdOrPrtry;
        }

        public void setCdOrPrtry(CodeOrProprietary cdOrPrtry) {
            this.cdOrPrtry = cdOrPrtry;
        }
}
