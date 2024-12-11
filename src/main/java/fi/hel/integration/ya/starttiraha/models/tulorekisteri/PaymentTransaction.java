package fi.hel.integration.ya.starttiraha.models.tulorekisteri;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class PaymentTransaction {
    
    @JacksonXmlProperty(localName = "TransactionBasic")
    private TransactionBasic transactionBasic;

    @JacksonXmlProperty(localName = "EarningPeriod")
    private EarningPeriod earningPeriod;

    // Getters and Setters
    public TransactionBasic getTransactionBasic() {
        return transactionBasic;
    }

    public void setTransactionBasic(TransactionBasic transactionBasic) {
        this.transactionBasic = transactionBasic;
    }

    public EarningPeriod getEarningPeriod() {
        return earningPeriod;
    }

    public void setEarningPeriod(EarningPeriod earningPeriod) {
        this.earningPeriod = earningPeriod;
    }
}
