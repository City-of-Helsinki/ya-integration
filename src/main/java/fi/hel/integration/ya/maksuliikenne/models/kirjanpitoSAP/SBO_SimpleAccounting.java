package fi.hel.integration.ya.maksuliikenne.models.kirjanpitoSAP;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class SBO_SimpleAccounting {
    
    @JacksonXmlProperty(localName = "SenderId")
    private String senderId;

    @JacksonXmlProperty(localName = "CompanyCode")
    private String companyCode;

    @JacksonXmlProperty(localName = "DocumentType")
    private String documentType;

    @JacksonXmlProperty(localName = "DocumentDate")
    private String documentDate;

    @JacksonXmlProperty(localName = "PostingDate")
    private String postingDate;

    @JacksonXmlProperty(localName = "Reference")
    private String reference;

    @JacksonXmlProperty(localName = "HeaderText")
    private String headerText;

    @JacksonXmlProperty(localName = "CurrencyCode")
    private String currencyCode;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "LineItem")
    private List<LineItemType> lineItem;

    // Getters and Setters
    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentDate() {
        return documentDate;
    }

    public void setDocumentDate(String documentDate) {
        this.documentDate = documentDate;
    }

    public String getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(String postingDate) {
        this.postingDate = postingDate;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getHeaderText() {
        return headerText;
    }

    public void setHeaderText(String headerText) {
        this.headerText = headerText;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public List<LineItemType> getLineItem() {
        return lineItem;
    }

    public void setLineItem(List<LineItemType> lineItem) {
        this.lineItem = lineItem;
    }
}
