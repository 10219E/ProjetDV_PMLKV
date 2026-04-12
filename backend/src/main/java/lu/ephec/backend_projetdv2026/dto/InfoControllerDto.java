package lu.ephec.backend_projetdv2026.dto;

public class InfoControllerDto {

    private Integer sites;
    private Integer fields;

    public InfoControllerDto(Integer sites, Integer fields) {
        this.sites = sites;
        this.fields = fields;
    }

    // Public getters so Jackson can serialize the DTO in responses
    public Integer getSites() {
        return sites;
    }

    public Integer getFields() {
        return fields;
    }
}
