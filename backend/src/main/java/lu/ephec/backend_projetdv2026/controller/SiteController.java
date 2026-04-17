package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.SiteDto;
import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.services.SiteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping(produces = "application/json")
    public ResponseEntity<List<SiteDto>> getAllSites(@RequestParam(required = false, defaultValue = "true") boolean activeOnly) {
        List<Site> sites = activeOnly ? siteService.fetchAllActive() : siteService.fetchAll();
        List<SiteDto> responses = sites.stream()
                .map(SiteDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value="/{id}", produces = "application/json")
    public ResponseEntity<SiteDto> getSiteById(@PathVariable Integer id) {
        Site site = siteService.fetchById(id).orElseThrow();
        return ResponseEntity.ok(SiteDto.from(site));
    }
}

