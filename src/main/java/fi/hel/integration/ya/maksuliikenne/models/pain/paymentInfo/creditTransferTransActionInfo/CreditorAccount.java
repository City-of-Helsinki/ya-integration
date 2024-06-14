package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.AccountIdentification;

public class CreditorAccount {
    @JacksonXmlProperty(localName = "Id")
    private AccountIdentification id;

    public AccountIdentification getId() {
        return id;
    }

    public void setId(AccountIdentification id) {
        this.id = id;
    }
}
