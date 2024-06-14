package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class DebtorAccount {
    @JacksonXmlProperty(localName = "Id")
    private AccountIdentification id;

    public AccountIdentification getId() {
        return id;
    }

    public void setId(AccountIdentification id) {
        this.id = id;
    }
}
