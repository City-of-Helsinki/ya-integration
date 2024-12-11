package fi.hel.integration.ya.starttiraha.models.tulorekisteri;

import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class IncomeEarner {
    
    @JacksonXmlElementWrapper(localName = "IncomeEarnerIds")
    @JacksonXmlProperty(localName = "Id")
    private List<Id> incomeEarnerIds;

    // Getters and Setters
    public List<Id> getIncomeEarnerIds() {
        return incomeEarnerIds;
    }

    public void setIncomeEarnerIds(List<Id> incomeEarnerIds) {
        this.incomeEarnerIds = incomeEarnerIds;
    }
}
