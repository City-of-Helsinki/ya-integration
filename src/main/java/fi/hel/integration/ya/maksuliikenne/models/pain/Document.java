package fi.hel.integration.ya.maksuliikenne.models.pain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "Document") //, namespace = "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03")
public class Document {

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsi")
    private String xsi;

    @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsd")
    private String xsd;

    @JacksonXmlProperty(isAttribute = true, localName = "xsi:schemaLocation")
    private String schemaLocation;
    
    @JacksonXmlProperty(isAttribute = true, localName = "xmlns")
    private String xmlns;

    @JacksonXmlProperty(localName = "CstmrCdtTrfInitn")
    private MessageRoot messageRoot;

    public MessageRoot getMessageRoot() {
        return messageRoot;
    }

    public void setMessageRoot(MessageRoot messageRoot) {
        this.messageRoot = messageRoot;
    }

    public String getXsi() {
        return xsi;
    }

    public String getXsd() {
        return xsd;
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

    public String getXmlns() {
        return xmlns;
    }

    public void setXsi(String xsi) {
        this.xsi = xsi;
    }

    public void setXsd(String xsd) {
        this.xsd = xsd;
    }

    public void setSchemaLocation(String schemaLocation) {
        this.schemaLocation = schemaLocation;
    }

    public void setXmlns(String xmlns) {
        this.xmlns = xmlns;
    }
}
