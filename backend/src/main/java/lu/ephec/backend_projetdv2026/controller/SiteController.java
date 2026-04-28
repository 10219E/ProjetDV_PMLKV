package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.SiteDto;
import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.services.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteService siteService;
    private static final Logger logger = LoggerFactory.getLogger(SiteController.class);

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    @GetMapping(produces = "application/json")
    public ResponseEntity<List<SiteDto>> getAllSites(@RequestParam(required = false, defaultValue = "true") boolean activeOnly) {
        logger.debug("[SITE CONTROLLER] Getting all sites called with activeOnly={}", activeOnly);
        List<Site> sites = activeOnly ? siteService.fetchAllActive() : siteService.fetchAll();
        List<SiteDto> responses = sites.stream()
                .map(site -> {
                    // fetch sessions for each site (may be parsed JSON as List)
                    List<?> sessions = null;
                    try {
                        sessions = siteService.fetchSessionTimesForSite(site.getSiteId());
                    } catch (Exception ex) {
                        // if sessions not found or parsing fails, keep null
                        logger.warn("[SITE CONTROLLER] Failed to fetch sessions for site {} — leaving sessions=null", site.getSiteId(), ex);
                    }
                    return SiteDto.from(site, sessions);
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value="/{id}", produces = "application/json")
    public ResponseEntity<SiteDto> getSiteById(@PathVariable Integer id) {
        logger.debug("[SITE CONTROLLER] Getting site called with id={}", id);
        Site site = siteService.fetchById(id).orElseThrow();
        List<?> sessions = null;
        try {
            sessions = siteService.fetchSessionTimesForSite(site.getSiteId());
        } catch (Exception ex) {
            // leave sessions null if not found / parsing error
            logger.warn("[SITE CONTROLLER] Failed to fetch sessions for site {} (id={}) — leaving sessions=null", site.getSiteId(), id, ex);
        }
        return ResponseEntity.ok(SiteDto.from(site, sessions));
    }
}

