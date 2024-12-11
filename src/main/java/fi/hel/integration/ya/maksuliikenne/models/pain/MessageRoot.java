package fi.hel.integration.ya.maksuliikenne.models.pain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import fi.hel.integration.ya.maksuliikenne.models.pain.groupHeader.GroupHeader;
import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.PaymentInfo;

public class MessageRoot {
    @JacksonXmlProperty(localName = "GrpHdr")
    private GroupHeader groupHeader;

    @JacksonXmlProperty(localName = "PmtInf")
    private PaymentInfo paymentInformation;

    public GroupHeader getGroupHeader() {
        return groupHeader;
    }

    public void setGroupHeader(GroupHeader groupHeader) {
        this.groupHeader = groupHeader;
    }

    public PaymentInfo getPaymentInformation() {
       return paymentInformation;
    }

    public void setPaymentInformation(PaymentInfo paymentInformation) {
       this.paymentInformation = paymentInformation;
    }
}
