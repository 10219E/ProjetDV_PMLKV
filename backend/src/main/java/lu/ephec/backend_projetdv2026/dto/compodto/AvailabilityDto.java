package lu.ephec.backend_projetdv2026.dto.compodto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AvailabilityDto {
    private Integer siteId;
    private Integer fieldId;
    private LocalDate date;
    private List<SessionDto> availableSessions;

    // Constructors
    public AvailabilityDto() {
    }

    public AvailabilityDto(Integer siteId, Integer fieldId, LocalDate date, List<SessionDto> availableSessions) {
        this.siteId = siteId;
        this.fieldId = fieldId;
        this.date = date;
        this.availableSessions = availableSessions;
    }

    // Getters and Setters
    public Integer getSiteId() {return siteId;}

    public void setSiteId(Integer siteId) {this.siteId = siteId;}

    public Integer getFieldId() {return fieldId;}

    public void setFieldId(Integer fieldId) {this.fieldId = fieldId;}

    public LocalDate getDate() {return date;}

    public void setDate(LocalDate date) {this.date = date;}

    public List<SessionDto> getAvailableSessions() {return availableSessions;}

    public void setAvailableSessions(List<SessionDto> availableSessions) {this.availableSessions = availableSessions;}

    // Inner class for Session information
    public static class SessionDto {
        private LocalTime startTime;
        private LocalTime endTime;

        // Constructors
        public SessionDto() {
        }

        public SessionDto(LocalTime startTime, LocalTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        // Getters and Setters
        public LocalTime getStartTime() {return startTime;}

        public void setStartTime(LocalTime startTime) {this.startTime = startTime;}

        public LocalTime getEndTime() {return endTime;}

        public void setEndTime(LocalTime endTime) {this.endTime = endTime;}
    }
}