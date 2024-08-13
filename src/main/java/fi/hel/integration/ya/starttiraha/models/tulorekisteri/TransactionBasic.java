package fi.hel.integration.ya.starttiraha.models.tulorekisteri;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class TransactionBasic {
    
    @JacksonXmlProperty(localName = "TransactionCode")
    private String transactionCode;

    @JacksonXmlProperty(localName = "Amount")
    private String amount;

    // Getters and Setters
    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }
}
