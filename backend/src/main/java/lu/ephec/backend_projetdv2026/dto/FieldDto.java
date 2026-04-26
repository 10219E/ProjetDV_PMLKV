package lu.ephec.backend_projetdv2026.dto;

import lu.ephec.backend_projetdv2026.models.Field;
import java.time.LocalDate;

public class FieldDto {
    private Integer fieldId;
    private Integer siteId;
    private Boolean isIndoor;
    private Boolean isActive;
    private LocalDate maintenanceFromDate;
    private LocalDate maintenanceToDate;

    public FieldDto() {}

    public FieldDto(Integer fieldId, Integer siteId, Boolean isIndoor, Boolean isActive, LocalDate maintenanceFromDate, LocalDate maintenanceToDate) {
        this.fieldId = fieldId;
        this.siteId = siteId;
        this.isIndoor = isIndoor;
        this.isActive = isActive;
        this.maintenanceFromDate = maintenanceFromDate;
        this.maintenanceToDate = maintenanceToDate;
    }

    public static FieldDto from(Field field) {
        return new FieldDto(
                field.getFieldId(),
                field.getSite() != null ? field.getSite().getSiteId() : null,
                field.getIsIndoor(),
                field.getIsActive(),
                field.getMaintenanceFromDate(),
                field.getMaintenanceToDate()
        );
    }

    public Integer getFieldId() { return fieldId; }
    public void setFieldId(Integer fieldId) { this.fieldId = fieldId; }
    public Integer getSiteId() { return siteId; }
    public void setSiteId(Integer siteId) { this.siteId = siteId; }
    public Boolean getIsIndoor() { return isIndoor; }
    public void setIsIndoor(Boolean isIndoor) { this.isIndoor = isIndoor; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDate getMaintenanceFromDate() { return maintenanceFromDate; }
    public void setMaintenanceFromDate(LocalDate maintenanceFromDate) { this.maintenanceFromDate = maintenanceFromDate; }
    public LocalDate getMaintenanceToDate() { return maintenanceToDate; }
    public void setMaintenanceToDate(LocalDate maintenanceToDate) { this.maintenanceToDate = maintenanceToDate; }
}

