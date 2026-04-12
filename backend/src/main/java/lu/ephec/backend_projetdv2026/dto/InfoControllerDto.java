package lu.ephec.backend_projetdv2026.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.List;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InfoControllerDto {
    private final Integer sites;
    private final Integer fields;
    private final List<SiteInfo> siteInfoList;

    // Constructor for counts
    public InfoControllerDto(Integer sites, Integer fields) {
        this.sites = sites;
        this.fields = fields;
        this.siteInfoList = null;
    }

    // Constructor for site list
    public InfoControllerDto(List<SiteInfo> siteInfoList) {
        this.sites = null;
        this.fields = null;
        this.siteInfoList = siteInfoList;
    }

    @Getter
    public static class SiteInfo {
        private final Integer siteId;
        private final String name;
        private final String address;

        public SiteInfo(Integer siteId, String name, String address) {
            this.siteId = siteId;
            this.name = name;
            this.address = address;
        }
    }
}
