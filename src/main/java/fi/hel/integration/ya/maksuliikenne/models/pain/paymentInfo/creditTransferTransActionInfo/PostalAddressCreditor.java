package fi.hel.integration.ya.maksuliikenne.models.pain.paymentInfo.creditTransferTransActionInfo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class PostalAddressCreditor {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JacksonXmlProperty(localName = "Ctry")
    private String ctry;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "AdrLine")
    private List<String> adrLine;

    public String getCtry() {
        return ctry;
    }

    public void setCtry(String ctry) {
        this.ctry = ctry;
    }

    public List<String> getAdrLine() {
        return adrLine;
    }

    public void setAdrLine(List<String> adrLine) {
        this.adrLine = adrLine;
    }
}
