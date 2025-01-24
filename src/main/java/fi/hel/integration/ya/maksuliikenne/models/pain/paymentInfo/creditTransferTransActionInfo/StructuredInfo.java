package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class StructuredInfo {
    
    @JacksonXmlProperty(localName = "CdtrRefInf")
        private CreditorReferenceInfo cdtrRefInf;

        public CreditorReferenceInfo getCdtrRefInf() {
            return cdtrRefInf;
        }

        public void setCdtrRefInf(CreditorReferenceInfo cdtrRefInf) {
            this.cdtrRefInf = cdtrRefInf;
        }  
}
