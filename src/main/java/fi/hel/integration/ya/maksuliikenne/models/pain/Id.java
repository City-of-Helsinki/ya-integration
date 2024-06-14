package fi.hel.integration.ya.maksuliikenne.models.pain;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Id {
    
    @JacksonXmlProperty(localName = "OrgId")
    private OrgId orgId;

    public OrgId getOrgId() {
        return orgId;
    }

    public void setOrgId(OrgId orgId) {
        this.orgId = orgId;
    }
}
