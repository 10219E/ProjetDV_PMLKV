package lu.ephec.backend_projetdv2026.dto;

public class UserSiteDto {
    private Integer siteId;
    private String siteName;
    private Boolean isPrimary;
    private Boolean isVip;

    public UserSiteDto() {}

    public UserSiteDto(Integer siteId, String siteName, Boolean isPrimary, Boolean isVip) {
        this.siteId = siteId;
        this.siteName = siteName;
        this.isPrimary = isPrimary;
        this.isVip = isVip;
    }

    public Integer getSiteId() { return siteId; }
    public void setSiteId(Integer siteId) { this.siteId = siteId; }
    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }
    public Boolean getIsVip() { return isVip; }
    public void setIsVip(Boolean isVip) { this.isVip = isVip; }
}

