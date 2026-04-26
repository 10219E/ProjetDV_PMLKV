package lu.ephec.backend_projetdv2026.dto;

import lu.ephec.backend_projetdv2026.models.Site;
import java.time.LocalTime;
import java.util.List;

public class SiteDto {
    private Integer siteId;
    private String name;
    private String address;
    private LocalTime openingTime;
    private LocalTime closingTime;
    private Boolean isActive;
    // Parsed sessions for the site (may be null when not requested)
    private List<?> sessions;

    public SiteDto() {
    }

    public SiteDto(Integer siteId, String name, String address, LocalTime openingTime, LocalTime closingTime, Boolean isActive) {
        this.siteId = siteId;
        this.name = name;
        this.address = address;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
        this.isActive = isActive;
    }

    // Overloaded constructor including sessions
    public SiteDto(Integer siteId, String name, String address, LocalTime openingTime, LocalTime closingTime, Boolean isActive, List<?> sessions) {
        this(siteId, name, address, openingTime, closingTime, isActive);
        this.sessions = sessions;
    }

    public static SiteDto from(Site site) {
        return new SiteDto(
                site.getSiteId(),
                site.getName(),
                site.getAddress(),
                site.getOpeningTime(),
                site.getClosingTime(),
                site.getIsActive()
        );
    }


    public static SiteDto from(Site site, List<?> sessions) {
        return new SiteDto(
                site.getSiteId(),
                site.getName(),
                site.getAddress(),
                site.getOpeningTime(),
                site.getClosingTime(),
                site.getIsActive(),
                sessions
        );
    }

    public Integer getSiteId() { return siteId; }
    public void setSiteId(Integer siteId) { this.siteId = siteId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalTime getOpeningTime() { return openingTime; }
    public void setOpeningTime(LocalTime openingTime) { this.openingTime = openingTime; }
    public LocalTime getClosingTime() { return closingTime; }
    public void setClosingTime(LocalTime closingTime) { this.closingTime = closingTime; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public List<?> getSessions() { return sessions; }
    public void setSessions(List<?> sessions) { this.sessions = sessions; }
}

