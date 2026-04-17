package lu.ephec.backend_projetdv2026.dto;

import java.util.List;

public class MatchCreationDto {
    private Integer fieldId;
    private String type;
    private String pubStatus;
    private String privStatus;
    private String matchDate; // yyyy-MM-dd
    private String startTime; // HH:mm[:ss]
    private String endTime;   // HH:mm[:ss]
    private Integer pricing;
    private String organiserId;
    private List<String> invites;

    public Integer getFieldId() { return fieldId; }
    public void setFieldId(Integer fieldId) { this.fieldId = fieldId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPubStatus() { return pubStatus; }
    public void setPubStatus(String pubStatus) { this.pubStatus = pubStatus; }
    public String getPrivStatus() { return privStatus; }
    public void setPrivStatus(String privStatus) { this.privStatus = privStatus; }
    public String getMatchDate() { return matchDate; }
    public void setMatchDate(String matchDate) { this.matchDate = matchDate; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public Integer getPricing() { return pricing; }
    public void setPricing(Integer pricing) { this.pricing = pricing; }
    public String getOrganiserId() { return organiserId; }
    public void setOrganiserId(String organiserId) { this.organiserId = organiserId; }
    public List<String> getInvites() { return invites; }
    public void setInvites(List<String> invites) { this.invites = invites; }
}

