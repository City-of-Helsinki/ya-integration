package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class CodeOrProprietary {
    
    @JacksonXmlProperty(localName = "Cd")
        private String cd;

        public String getCd() {
            return cd;
        }

        public void setCd(String cd) {
            this.cd = cd;
        }
}
