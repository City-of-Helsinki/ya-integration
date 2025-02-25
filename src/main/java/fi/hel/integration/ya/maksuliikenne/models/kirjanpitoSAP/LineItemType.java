package fi.hel.integration.ya.maksuliikenne.models.kirjanpitoSAP;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class LineItemType {
    
    @JacksonXmlProperty(localName = "AccountType")
    private String accountType;

    @JacksonXmlProperty(localName = "DebitOrCredit")
    private String debitOrCredit;

    @JacksonXmlProperty(localName = "TaxCode")
    private String taxCode;

    @JacksonXmlProperty(localName = "CondKey")
    private String condKey; // Optional

    @JacksonXmlProperty(localName = "AmountInDocumentCurrency")
    private String amountInDocumentCurrency;

    @JacksonXmlProperty(localName = "BaseAmount")
    private String baseAmount;

    @JacksonXmlProperty(localName = "ExchangeRate")
    private String exchangeRate;

    @JacksonXmlProperty(localName = "LineText")
    private String lineText;

    @JacksonXmlProperty(localName = "CompanyCode")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String companyCode;

    @JacksonXmlProperty(localName = "TradingPartner")
    private String tradingPartner;

    @JacksonXmlProperty(localName = "CostCenter")
    private String costCenter;

    @JacksonXmlProperty(localName = "Network")
    private String network;

    @JacksonXmlProperty(localName = "Phases")
    private String phases;

    @JacksonXmlProperty(localName = "OrderItemNumber")
    private String orderItemNumber;

    @JacksonXmlProperty(localName = "ReferenceId")
    private String referenceId;

    @JacksonXmlProperty(localName = "GLAccount")
    private String glAccount;

    @JacksonXmlProperty(localName = "Material")
    private String material;

    @JacksonXmlProperty(localName = "Quantity")
    private String quantity;

    @JacksonXmlProperty(localName = "BaseUnitOfMeasurement")
    private String baseUnitOfMeasurement;

    @JacksonXmlProperty(localName = "ProfitCenter")
    private String profitCenter;

    @JacksonXmlProperty(localName = "PartnerProfitCenter")
    private String partnerProfitCenter;

    @JacksonXmlProperty(localName = "WBS_Element")
    private String wbsElement;

    @JacksonXmlProperty(localName = "FunctionalArea")
    private String functionalArea;

    @JacksonXmlProperty(localName = "AssetNo")
    private String assetNo;

    @JacksonXmlProperty(localName = "AssetSubno")
    private String assetSubno;

    @JacksonXmlProperty(localName = "BusinessEntity")
    private String businessEntity;

    @JacksonXmlProperty(localName = "Building")
    private String building;

    @JacksonXmlProperty(localName = "Property")
    private String property;

    @JacksonXmlProperty(localName = "RentalObject")
    private String rentalObject;

    @JacksonXmlProperty(localName = "Fund")
    private String fund;

    // Getters and Setters

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getDebitOrCredit() {
        return debitOrCredit;
    }

    public void setDebitOrCredit(String debitOrCredit) {
        this.debitOrCredit = debitOrCredit;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public void setTaxCode(String taxCode) {
        this.taxCode = taxCode;
    }

    public String getCondKey() {
        return condKey;
    }

    public void setCondKey(String condKey) {
        this.condKey = condKey;
    }

    public String getAmountInDocumentCurrency() {
        return amountInDocumentCurrency;
    }

    public void setAmountInDocumentCurrency(String amountInDocumentCurrency) {
        this.amountInDocumentCurrency = amountInDocumentCurrency;
    }

    public String getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(String baseAmount) {
        this.baseAmount = baseAmount;
    }

    public String getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(String exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public String getLineText() {
        return lineText;
    }

    public void setLineText(String lineText) {
        this.lineText = lineText;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getTradingPartner() {
        return tradingPartner;
    }

    public void setTradingPartner(String tradingPartner) {
        this.tradingPartner = tradingPartner;
    }

    public String getCostCenter() {
        return costCenter;
    }

    public void setCostCenter(String costCenter) {
        this.costCenter = costCenter;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getPhases() {
        return phases;
    }

    public void setPhases(String phases) {
        this.phases = phases;
    }

    public String getOrderItemNumber() {
        return orderItemNumber;
    }

    public void setOrderItemNumber(String orderItemNumber) {
        this.orderItemNumber = orderItemNumber;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getGlAccount() {
        return glAccount;
    }

    public void setGlAccount(String glAccount) {
        this.glAccount = glAccount;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getBaseUnitOfMeasurement() {
        return baseUnitOfMeasurement;
    }

    public void setBaseUnitOfMeasurement(String baseUnitOfMeasurement) {
        this.baseUnitOfMeasurement = baseUnitOfMeasurement;
    }

    public String getProfitCenter() {
        return profitCenter;
    }

    public void setProfitCenter(String profitCenter) {
        this.profitCenter = profitCenter;
    }

    public String getPartnerProfitCenter() {
        return partnerProfitCenter;
    }

    public void setPartnerProfitCenter(String partnerProfitCenter) {
        this.partnerProfitCenter = partnerProfitCenter;
    }

    public String getWbsElement() {
        return wbsElement;
    }

    public void setWbsElement(String wbsElement) {
        this.wbsElement = wbsElement;
    }

    public String getFunctionalArea() {
        return functionalArea;
    }

    public void setFunctionalArea(String functionalArea) {
        this.functionalArea = functionalArea;
    }

    public String getAssetNo() {
        return assetNo;
    }

    public void setAssetNo(String assetNo) {
        this.assetNo = assetNo;
    }

    public String getAssetSubno() {
        return assetSubno;
    }

    public void setAssetSubno(String assetSubno) {
        this.assetSubno = assetSubno;
    }

    public String getBusinessEntity() {
        return businessEntity;
    }

    public void setBusinessEntity(String businessEntity) {
        this.businessEntity = businessEntity;
    }

    public String getBuilding() {
        return building;
    }

    public void setBuilding(String building) {
        this.building = building;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getRentalObject() {
        return rentalObject;
    }

    public void setRentalObject(String rentalObject) {
        this.rentalObject = rentalObject;
    }

    public String getFund() {
        return fund;
    }

    public void setFund(String fund) {
        this.fund = fund;
    }
}

