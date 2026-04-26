package lu.ephec.backend_projetdv2026.dto;

import lu.ephec.backend_projetdv2026.models.Match;
import java.time.LocalDate;
import java.time.LocalTime;

public class MatchDto {
    private Integer matchId;
    private Integer fieldId;
    private String type;
    private String pubStatus;
    private String privStatus;
    private LocalDate matchDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer pricing;
    private String organiserId;

    public MatchDto() {}

    public MatchDto(Integer matchId, Integer fieldId, String type, String pubStatus, String privStatus, LocalDate matchDate, LocalTime startTime, LocalTime endTime, Integer minPlayers, Integer maxPlayers, Integer pricing, String organiserId) {
        this.matchId = matchId;
        this.fieldId = fieldId;
        this.type = type;
        this.pubStatus = pubStatus;
        this.privStatus = privStatus;
        this.matchDate = matchDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.pricing = pricing;
        this.organiserId = organiserId;
    }

    public static MatchDto from(Match m) {
        return new MatchDto(
                m.getMatchId(),
                m.getField() != null ? m.getField().getFieldId() : null,
                m.getType(),
                m.getPubStatus(),
                m.getPrivStatus(),
                m.getMatchDate(),
                m.getStartTime(),
                m.getEndTime(),
                m.getMinPlayers(),
                m.getMaxPlayers(),
                m.getPricing(),
                m.getOrganiser() != null ? m.getOrganiser().getMatricule() : null
        );
    }

    public Integer getMatchId() { return matchId; }
    public void setMatchId(Integer matchId) { this.matchId = matchId; }
    public Integer getFieldId() { return fieldId; }
    public void setFieldId(Integer fieldId) { this.fieldId = fieldId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getPubStatus() { return pubStatus; }
    public void setPubStatus(String pubStatus) { this.pubStatus = pubStatus; }
    public String getPrivStatus() { return privStatus; }
    public void setPrivStatus(String privStatus) { this.privStatus = privStatus; }
    public LocalDate getMatchDate() { return matchDate; }
    public void setMatchDate(LocalDate matchDate) { this.matchDate = matchDate; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Integer getPricing() { return pricing; }
    public void setPricing(Integer pricing) { this.pricing = pricing; }
    public String getOrganiserId() { return organiserId; }
    public void setOrganiserId(String organiserId) { this.organiserId = organiserId; }
}

