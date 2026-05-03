package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.FieldDto;
import lu.ephec.backend_projetdv2026.dto.SiteDto;
import lu.ephec.backend_projetdv2026.dto.compodto.MatchPlayerSiteFieldDto;
import lu.ephec.backend_projetdv2026.dto.MatchPlayerDto;
import lu.ephec.backend_projetdv2026.dto.MatchDto;
import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.models.MatchPlayers;
import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.services.MatchService;
import lu.ephec.backend_projetdv2026.services.SiteService;
import lu.ephec.backend_projetdv2026.services.sitefieldbymatch.SiteFieldsByMatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/match-players")
public class MatchPlayerController {

    private final MatchService matchService;
    private static final Logger logger = LoggerFactory.getLogger(MatchPlayerController.class);
    private final SiteFieldsByMatchService siteFieldsByMatchService;
    private final SiteService siteService;

    @Autowired
    public MatchPlayerController(MatchService matchService, SiteFieldsByMatchService siteFieldsByMatchService, SiteService siteService) {
        this.matchService = matchService;
        this.siteFieldsByMatchService = siteFieldsByMatchService;
        this.siteService = siteService;
    }

    @PutMapping(value = "/{id}", produces = "application/json")
    public MatchPlayerSiteFieldDto updateMatchPlayer(@PathVariable Integer id, @RequestBody MatchPlayerDto matchPlayerDto) {
        if (!id.equals(matchPlayerDto.getMatch().getMatchId())) {
            throw new IllegalArgumentException("Match ID in path must match matchId in request body");
        }

        MatchPlayerDto updatedPlayer = matchService.updateMatchPlayer(
                        matchPlayerDto.getMatch().getMatchId(),
                        matchPlayerDto.getUserMatricule(),
                        matchPlayerDto.getStatus()
                ).map(MatchPlayerDto::fromEntity)
                .orElseThrow(() -> new RuntimeException("MatchPlayer not found for match " + id));

        // Create and return composite DTO
        MatchPlayerSiteFieldDto details = new MatchPlayerSiteFieldDto();
        details.setPlayer(updatedPlayer);

        // Add additional match information if needed
        if (updatedPlayer.getMatch() != null) {
            details.setMatch(updatedPlayer.getMatch());
            // Add more match-related data as needed
        }

        return details;
    }

    @GetMapping(value = "/mymatches/{userMatricule}", produces = "application/json")
    public ResponseEntity<List<MatchPlayerSiteFieldDto>> getMyMatches(@PathVariable String userMatricule) {
        // Validate the userMatricule parameter
        if (userMatricule == null || userMatricule.trim().isEmpty()) {
            throw new IllegalArgumentException("User matricule cannot be null or empty");
        }

        logger.info("[MatchPlayerController] Fetching matches for user: {}", userMatricule);

        // Get the list of matches from the service
        List<Match> matches = matchService.fetchMyUpcomingMatches(userMatricule);
        logger.info("[MatchPlayerController] Found {} matches for user: {}", matches.size(), userMatricule);

        // Get the list of match players from the service
        List<MatchPlayers> matchPlayers = matchService.fetchMatchesByUserMatricule(userMatricule);
        logger.info("[MatchPlayerController] Found {} MatchPlayers entries for user: {}", matchPlayers.size(), userMatricule);

        // Create a map of match players by match ID for quick lookup
        Map<Integer, MatchPlayers> matchPlayersMap = matchPlayers.stream()
                .collect(Collectors.toMap(
                        mp -> mp.getMatch().getMatchId(),
                        mp -> mp,
                        (existing, replacement) -> existing // In case of duplicates, keep the first one
                ));

        // Get all unique sites and fields for these matches
        Map<Site, List<Field>> sitesAndFields = siteFieldsByMatchService.findSitesAndFieldsForMatches(matches);

        // Convert to MatchPlayerSiteFieldDto
        return ResponseEntity.ok(matches.stream()
                .map(match -> {
                    MatchPlayerSiteFieldDto details = new MatchPlayerSiteFieldDto();

                    // Set the match information
                    details.setMatch(MatchDto.from(match));

                    // Get the corresponding match player information
                    MatchPlayers matchPlayer = matchPlayersMap.get(match.getMatchId());

                    if (matchPlayer != null) {
                        // Create and set the player information
                        MatchPlayerDto playerDto = MatchPlayerDto.fromEntity(matchPlayer);
                        details.setPlayer(playerDto);
                    } else {
                        // If no match player found, create a default player DTO
                        MatchPlayerDto playerDto = new MatchPlayerDto();
                        playerDto.setMatch(MatchDto.from(match));
                        playerDto.setUserMatricule(userMatricule);
                        playerDto.setStatus("not_registered");
                        details.setPlayer(playerDto);
                    }

                    // Set the site and field information
                    if (match.getField() != null && match.getField().getSite() != null) {
                        Site site = match.getField().getSite();
                        details.setSite(SiteDto.from(site));

                        // Find the corresponding field in the sitesAndFields map
                        List<Field> fields = sitesAndFields.get(site);
                        if (fields != null) {
                            Field field = fields.stream()
                                    .filter(f -> f.getFieldId().equals(match.getField().getFieldId()))
                                    .findFirst()
                                    .orElse(null);

                            if (field != null) {
                                details.setField(FieldDto.from(field));
                            }
                        }
                    }

                    return details;
                })
                .collect(Collectors.toList()));
    }
}