package lu.ephec.backend_projetdv2026.dto;

import java.time.LocalDateTime;

public class UserPenaltyDto {
    private Integer tr;
    private String reason;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isActive;
    private String description;

    public UserPenaltyDto() {}

    public UserPenaltyDto(Integer tr, String reason, LocalDateTime startDate, LocalDateTime endDate, Boolean isActive, String description) {
        this.tr = tr;
        this.reason = reason;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isActive = isActive;
        this.description = description;
    }

    public Integer getTr() { return tr; }
    public void setTr(Integer tr) { this.tr = tr; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

