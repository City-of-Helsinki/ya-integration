package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class CreditorReferenceInfo {

    @JacksonXmlProperty(localName = "Tp")
        private Type tp;

        @JacksonXmlProperty(localName = "Ref")
        private String ref;

        public Type getTp() {
            return tp;
        }

        public void setTp(Type tp) {
            this.tp = tp;
        }

        public String getRef() {
            return ref;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }
}
