package fi.hel.integration.ya.starttiraha.models.tulorekisteri;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class DeliveryData {

    @JacksonXmlProperty(localName = "Timestamp")
    private String timestamp;

    @JacksonXmlProperty(localName = "Source")
    private String source;

    @JacksonXmlProperty(localName = "DeliveryDataType")
    private int deliveryDataType;

    @JacksonXmlProperty(localName = "DeliveryId")
    private String deliveryId;

    @JacksonXmlProperty(localName = "FaultyControl")
    private int faultyControl;

    @JacksonXmlProperty(localName = "ProductionEnvironment")
    private boolean productionEnvironment;

    @JacksonXmlProperty(localName = "DeliveryDataOwner")
    private DeliveryDataOwner deliveryDataOwner;

    @JacksonXmlProperty(localName = "DeliveryDataCreator")
    private DeliveryDataCreator deliveryDataCreator;

    @JacksonXmlProperty(localName = "DeliveryDataSender")
    private DeliveryDataSender deliveryDataSender;

    @JacksonXmlProperty(localName = "PaymentDate")
    private String paymentDate;

    @JacksonXmlElementWrapper(localName = "ContactPersons")
    @JacksonXmlProperty(localName = "ContactPerson")
    private List<ContactPerson> contactPersons;

    @JacksonXmlProperty(localName = "Payer")
    private Payer payer;

    @JacksonXmlElementWrapper(localName = "Reports")
    @JacksonXmlProperty(localName = "Report")
    private List<Report> reports;

    // Getters and Setters
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getDeliveryDataType() {
        return deliveryDataType;
    }

    public void setDeliveryDataType(int deliveryDataType) {
        this.deliveryDataType = deliveryDataType;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public int getFaultyControl() {
        return faultyControl;
    }

    public void setFaultyControl(int faultyControl) {
        this.faultyControl = faultyControl;
    }

    public boolean isProductionEnvironment() {
        return productionEnvironment;
    }

    public void setProductionEnvironment(boolean productionEnvironment) {
        this.productionEnvironment = productionEnvironment;
    }

    public DeliveryDataOwner getDeliveryDataOwner() {
        return deliveryDataOwner;
    }

    public void setDeliveryDataOwner(DeliveryDataOwner deliveryDataOwner) {
        this.deliveryDataOwner = deliveryDataOwner;
    }

    public DeliveryDataCreator getDeliveryDataCreator() {
        return deliveryDataCreator;
    }

    public void setDeliveryDataCreator(DeliveryDataCreator deliveryDataCreator) {
        this.deliveryDataCreator = deliveryDataCreator;
    }

    public DeliveryDataSender getDeliveryDataSender() {
        return deliveryDataSender;
    }

    public void setDeliveryDataSender(DeliveryDataSender deliveryDataSender) {
        this.deliveryDataSender = deliveryDataSender;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public List<ContactPerson> getContactPersons() {
        return contactPersons;
    }

    public void setContactPersons(List<ContactPerson> contactPersons) {
        this.contactPersons = contactPersons;
    }

    public Payer getPayer() {
        return payer;
    }

    public void setPayer(Payer payer) {
        this.payer = payer;
    }

    public List<Report> getReports() {
        return reports;
    }

    public void setReports(List<Report> reports) {
        this.reports = reports;
    }
}
    

