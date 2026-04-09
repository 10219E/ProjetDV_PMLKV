package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.SiteResponse;
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

    @GetMapping
    public ResponseEntity<List<SiteResponse>> getAllSites(@RequestParam(required = false, defaultValue = "true") boolean activeOnly) {
        List<Site> sites = activeOnly ? siteService.fetchAllActive() : siteService.fetchAll();
        List<SiteResponse> responses = sites.stream()
                .map(SiteResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SiteResponse> getSiteById(@PathVariable Integer id) {
        Site site = siteService.fetchById(id).orElseThrow();
        return ResponseEntity.ok(SiteResponse.from(site));
    }
}

