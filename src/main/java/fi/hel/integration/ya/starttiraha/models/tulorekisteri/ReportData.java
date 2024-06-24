package fi.hel.integration.ya.starttiraha.models.tulorekisteri;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class ReportData {
    
    @JacksonXmlProperty(localName = "ActionCode")
    private int actionCode;

    @JacksonXmlProperty(localName = "ReportId")
    private String reportId;

    // Getters and Setters
    public int getActionCode() {
        return actionCode;
    }

    public void setActionCode(int actionCode) {
        this.actionCode = actionCode;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }
}
