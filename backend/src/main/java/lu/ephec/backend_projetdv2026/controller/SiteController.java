package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.SiteDto;
import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.models.SiteClosureDays;
import lu.ephec.backend_projetdv2026.services.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
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

    @GetMapping(value = "/{id}/closures", produces = "application/json")
    public ResponseEntity<List<Map<String, Object>>> getClosuresForSite(@PathVariable Integer id) {
        logger.debug("[SITE CONTROLLER] Getting closures for site with id={}", id);
        List<SiteClosureDays> closures = siteService.fetchClosureForSite(id);
        // Return a simplified representation to avoid serializing nested Site -> closureDays recursion
        List<Map<String, Object>> simplified = (closures == null) ? List.of() : closures.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("closureId", c.getClosureId());
            m.put("siteId", c.getSiteId());
            m.put("closureDate", c.getClosureDate());
            m.put("reason", c.getReason());
            m.put("isForAll", c.getForAll());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(simplified);
    }

    @PostMapping(produces = "application/json", consumes = "application/json")
    public ResponseEntity<SiteDto> newSite(@RequestBody SiteDto siteDto) {
        logger.info("[SITE CONTROLLER] Creating new site");
        Site site = new Site();
        site.setName(siteDto.getName());
        site.setAddress(siteDto.getAddress());
        if (siteDto.getOpeningTime() != null) {
            site.setOpeningTime(siteDto.getOpeningTime());
        }
        if (siteDto.getClosingTime() != null) {
            site.setClosingTime(siteDto.getClosingTime());
        }
        site.setIsActive(siteDto.getIsActive() != null ? siteDto.getIsActive() : true);

        Site saved = siteService.newSite(site);

        List<?> sessions = null;
        try {
            sessions = siteService.fetchSessionTimesForSite(saved.getSiteId());
        } catch (Exception ex) {
            logger.warn("[SITE CONTROLLER] Failed to fetch sessions for site {} — leaving sessions=null", saved.getSiteId(), ex);
        }

        return ResponseEntity.ok(SiteDto.from(saved, sessions));
    }

    @PatchMapping(value = "/{siteId}", produces = "application/json")
    public ResponseEntity<SiteDto> updateSite(@PathVariable Integer siteId, @RequestBody Map<String, Object> updates) {
        logger.info("[SITE CONTROLLER] Updating site with id={}", siteId);

        Optional<Site> siteOpt = siteService.fetchById(siteId);
        if (siteOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Site updateData = new Site();

        if (updates.containsKey("siteName")) {
            updateData.setName((String) updates.get("siteName"));
        }

        if (updates.containsKey("siteAddress")) {
            updateData.setAddress((String) updates.get("siteAddress"));
        }

        if (updates.containsKey("openingTime")) {
            logger.warn("[SITE CONTROLLER] Received update for openingTime, process will have to recalculate sessions.");
            Object val = updates.get("openingTime");
            if (val instanceof String) {
                updateData.setOpeningTime(LocalTime.parse((String) val));
            } else if (val instanceof Map) {
                // If somehow it's still sent as an object
                Map<String, Integer> m = (Map<String, Integer>) val;
                updateData.setOpeningTime(LocalTime.of(m.getOrDefault("hour", 0), m.getOrDefault("minute", 0)));
            }
        }

        if (updates.containsKey("closingTime")) {
            Object val = updates.get("closingTime");
            if (val instanceof String) {
                updateData.setClosingTime(LocalTime.parse((String) val));
            } else if (val instanceof Map) {
                Map<String, Integer> m = (Map<String, Integer>) val;
                updateData.setClosingTime(LocalTime.of(m.getOrDefault("hour", 0), m.getOrDefault("minute", 0)));
            }
        }

        if (updates.containsKey("isActive")) {
            updateData.setIsActive((Boolean) updates.get("isActive"));
        }

        return siteService.updateSite(siteId, updateData)
                .map(updated -> {
                    List<?> sessions = null;
                    try {
                        sessions = siteService.fetchSessionTimesForSite(updated.getSiteId());
                    } catch (Exception ex) {
                        logger.warn("[SITE CONTROLLER] Failed to fetch sessions for site {} — leaving sessions=null", updated.getSiteId(), ex);
                    }
                    return ResponseEntity.ok(SiteDto.from(updated, sessions));
                })
                .orElse(ResponseEntity.badRequest().build());
    }
}

