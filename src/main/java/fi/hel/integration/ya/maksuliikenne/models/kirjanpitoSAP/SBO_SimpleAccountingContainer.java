package fi.hel.integration.ya.maksuliikenne.models.kirjanpitoSAP;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class SBO_SimpleAccountingContainer {
    
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "SBO_SimpleAccounting")
    private List<SBO_SimpleAccounting> sboSimpleAccounting;

    // Getters and Setters
    public List<SBO_SimpleAccounting> getSboSimpleAccounting() {
        return sboSimpleAccounting;
    }

    public void setSboSimpleAccounting(List<SBO_SimpleAccounting> sboSimpleAccounting) {
        this.sboSimpleAccounting = sboSimpleAccounting;
    }
}
