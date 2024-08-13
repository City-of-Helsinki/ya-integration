package fi.hel.integration.ya.starttiraha.models.tulorekisteri;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "brtir:BenefitReportsRequestToIR")
public class BenefitReportsRequestToIR {

    @JacksonXmlProperty(isAttribute = true, localName = " xmlns:brtir")
    private String brtir;

    
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsi")
    private String xsi;

    
    @JacksonXmlProperty(isAttribute = true, localName = "xsi:schemaLocation")
    private String schemaLocation;
    
    @JacksonXmlProperty(localName = "DeliveryData")
    private DeliveryData deliveryData;

    // Getters and Setters
    public DeliveryData getDeliveryData() {
        return deliveryData;
    }

    public void setDeliveryData(DeliveryData deliveryData) {
        this.deliveryData = deliveryData;
    }

    public String getXsi() {
        return xsi;
    }

    public void setXsi(String xsi) {
        this.xsi = xsi;
    }

    public String getBrtir() {
        return brtir;
    }

    public void setBrtir(String brtir) {
        this.brtir = brtir;
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
    }
}
