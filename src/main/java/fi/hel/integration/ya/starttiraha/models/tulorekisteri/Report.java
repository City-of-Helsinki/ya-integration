package fi.hel.integration.ya.starttiraha.models.tulorekisteri;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Report {
    
    @JacksonXmlProperty(localName = "ReportData")
    private ReportData reportData;

    @JacksonXmlProperty(localName = "IncomeEarner")
    private IncomeEarner incomeEarner;

    @JacksonXmlElementWrapper(localName = "Transactions")
    @JacksonXmlProperty(localName = "Transaction")
    private List<PaymentTransaction> transactions;

    // Getters and Setters
    public ReportData getReportData() {
        return reportData;
    }

    public void setReportData(ReportData reportData) {
        this.reportData = reportData;
    }

    public IncomeEarner getIncomeEarner() {
        return incomeEarner;
    }

    public void setIncomeEarner(IncomeEarner incomeEarner) {
        this.incomeEarner = incomeEarner;
    }

    public List<PaymentTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<PaymentTransaction> transactions) {
        this.transactions = transactions;
    }
}
