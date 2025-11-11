package fi.hel.integration.ya.maksuliikenne.Maksupaivapalaute;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaymentFeedback {
    
    @JsonProperty("invoiceNumber")
    private Long invoiceNumber;
    
    @JsonProperty("state")
    private String state;
    
    @JsonProperty("paidAt")
    private String paidAt;
    
    @JsonProperty("receiptDate")
    private String receiptDate;
    
    public PaymentFeedback() {
    }
    
    public PaymentFeedback(Long invoiceNumber, String state, String paidAt, String receiptDate) {
        this.invoiceNumber = invoiceNumber;
        this.state = state;
        this.paidAt = paidAt;
        this.receiptDate = receiptDate;
    }
    
    public Long getInvoiceNumber() {
        return invoiceNumber;
    }
    
    public void setInvoiceNumber(Long invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }
    
    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }
    
    public String getPaidAt() {
        return paidAt;
    }
    
    public void setPaidAt(String paidAt) {
        this.paidAt = paidAt;
    }
    
    public String getReceiptDate() {
        return receiptDate;
    }
    
    public void setReceiptDate(String receiptDate) {
        this.receiptDate = receiptDate;
    }
}