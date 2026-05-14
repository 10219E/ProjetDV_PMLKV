package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.FieldDto;
import lu.ephec.backend_projetdv2026.dto.MatchDto;
import lu.ephec.backend_projetdv2026.dto.SiteDto;
import lu.ephec.backend_projetdv2026.dto.compodto.MatchSiteFieldDto;
import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.services.MatchService;
import lu.ephec.backend_projetdv2026.services.sitefieldbymatch.SiteFieldsByMatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;
    private final SiteFieldsByMatchService siteFieldsByMatchService;
    private final Logger logger = LoggerFactory.getLogger(MatchController.class);

    public MatchController(MatchService matchService, SiteFieldsByMatchService siteFieldsByMatchService) {
        this.matchService = matchService;
        this.siteFieldsByMatchService = siteFieldsByMatchService;
    }

    @GetMapping(produces = "application/json")
    public ResponseEntity<List<MatchDto>> getAll() {
        List<MatchDto> responses = matchService.fetchAll().stream()
                .map(MatchDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    //@PostMapping(produces = "application/json")
    //public ResponseEntity<MatchDto> createMatch(@RequestBody Match match, @RequestParam(value = "invite", required = false) List<String> invites) {
    //    Match created = matchService.newMatch(match, invites);
    //    return ResponseEntity.ok(MatchDto.from(created));
    //}

    @GetMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<MatchDto> getById(@PathVariable Integer id) {
        Match m = matchService.fetchById(id).orElseThrow();
        return ResponseEntity.ok(MatchDto.from(m));
    }

    @PutMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<MatchDto> updateMatch(@PathVariable Integer id, @RequestBody Match updateData) {
        Optional<Match> updated = matchService.updateMatch(id, updateData);
        return updated.map(m -> ResponseEntity.ok(MatchDto.from(m)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // DELETE only used by tests
    //@DeleteMapping(value = "/{id}")
    //public ResponseEntity<Void> deleteMatch(@PathVariable Integer id) {
    //    matchService.deleteMatch(id);
    //    return ResponseEntity.noContent().build();
    //}

    @GetMapping(value = "/type/{type}", produces = "application/json")
    public ResponseEntity<List<MatchDto>> getByType(@PathVariable String type) {
        List<MatchDto> responses = matchService.fetchByType(type).stream()
                .map(MatchDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value = "/type/{type}/status/{status}", produces = "application/json")
    public ResponseEntity<List<MatchDto>> getByTypeAndStatus(@PathVariable String type, @PathVariable String status) {
        List<MatchDto> responses = matchService.fetchMatchesByTypeAndStatus(type, status).stream()
                .map(MatchDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value = "/date", produces = "application/json")
    public ResponseEntity<List<MatchDto>> getByDateRange(@RequestParam("startDate") String startDate, @RequestParam("endDate") String endDate) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<MatchDto> responses = matchService.fetchByDateRange(start, end).stream()
                .map(MatchDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value = "/organiser/{organiserId}", produces = "application/json")
    public ResponseEntity<List<MatchDto>> getByOrganiser(@PathVariable String organiserId) {
        List<MatchDto> responses = matchService.fetchByOrganiser(organiserId).stream()
                .map(MatchDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value = "/site/{siteId}", produces = "application/json")
    public ResponseEntity<List<MatchDto>> getBySite(@PathVariable Integer siteId) {
        List<MatchDto> responses = matchService.fetchBySite(siteId).stream()
                .map(MatchDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value = "/colliding/{userMatricule}", produces = "application/json") //improved logic check -90 / +90 min range from start time
    public ResponseEntity<Boolean> getCollidingMatches(@PathVariable("userMatricule") String userMatricule, @RequestParam("matchDate") String matchDate, @RequestParam("startTime") String startTime) {
        List<Match> mymatches = matchService.fetchMyUpcomingMatches(userMatricule);
        LocalTime requestedTime = LocalTime.parse(startTime);
        boolean isColliding = mymatches.stream()
                .anyMatch(match -> {
                    if (!match.getMatchDate().toString().equals(matchDate)) {
                        return false;
                    }
                    LocalTime existingTime = LocalTime.parse(match.getStartTime().toString());
                    long diffMinutes = Math.abs(ChronoUnit.MINUTES.between(existingTime, requestedTime));
                    return diffMinutes < 90;
                });
        return ResponseEntity.ok(isColliding);
    }

    @GetMapping(value = "/mypublicmatches/{userMatricule}", produces = "application/json")
    public ResponseEntity<List<MatchSiteFieldDto>> getAvailablePublicMatches(@PathVariable String userMatricule) {
        // Validate the userMatricule parameter
        if (userMatricule == null || userMatricule.trim().isEmpty()) {
            throw new IllegalArgumentException("User matricule cannot be null or empty");
        }

        logger.info("[MatchController] Fetching available public matches with site and field info for user: {}", userMatricule);

        // Get the list of available public matches from the service
        List<Match> matches = matchService.fetchAvailablePublicMatches(userMatricule);
        logger.info("[MatchController] Found {} available public matches for user: {}", matches.size(), userMatricule);

        // Get all sites and fields for these matches
        Map<Site, List<Field>> sitesAndFields = siteFieldsByMatchService.findSitesAndFieldsForMatches(matches);

        // Convert to MatchSiteFieldDto
        List<MatchSiteFieldDto> responses = matches.stream()
                .map(match -> {
                    MatchSiteFieldDto dto = new MatchSiteFieldDto();

                    // Set the match information
                    dto.setMatch(MatchDto.from(match));

                    // Find the corresponding field and site
                    if (match.getField() != null) {
                        // Find the site for this field
                        Site site = match.getField().getSite();
                        if (site != null) {
                            // Set the site information
                            dto.setSite(SiteDto.from(site));

                            // Find the field in the sitesAndFields map
                            List<Field> fields = sitesAndFields.get(site);
                            if (fields != null) {
                                Field field = fields.stream()
                                        .filter(f -> f.getFieldId().equals(match.getField().getFieldId()))
                                        .findFirst()
                                        .orElse(null);

                                if (field != null) {
                                    // Set the field information
                                    dto.setField(FieldDto.from(field));
                                }
                            }
                        }
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

}

