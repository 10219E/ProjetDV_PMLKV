package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.MatchPlayerDto;
import lu.ephec.backend_projetdv2026.dto.MatchDto;
import lu.ephec.backend_projetdv2026.dto.UserProfileDto;
import lu.ephec.backend_projetdv2026.services.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/match-players")
public class MatchPlayerController {

    private final MatchService matchService;

    @Autowired
    public MatchPlayerController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PutMapping(value= "/{id}", produces = "application/json")
    public MatchPlayerDetails updateMatchPlayer(@PathVariable Integer id, @RequestBody MatchPlayerDto matchPlayerDto) {
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
        MatchPlayerDetails details = new MatchPlayerDetails();
        details.setPlayer(updatedPlayer);

        // Add additional match information if needed
        if (updatedPlayer.getMatch() != null) {
            details.setMatch(updatedPlayer.getMatch());
            // Add more match-related data as needed
        }

        return details;
    }

    @GetMapping(value = "/mymatches/{userMatricule}", produces = "application/json")
    public List<MatchPlayerDetails> getMyMatches(@PathVariable String userMatricule) {
        return matchService.fetchMatchesByUserMatricule(userMatricule).stream()
                .map(matchPlayer -> {
                    MatchPlayerDetails details = new MatchPlayerDetails();
                    details.setPlayer(MatchPlayerDto.fromEntity(matchPlayer));

                    // Add additional match information if needed
                    if (matchPlayer.getMatch() != null) {
                        details.setMatch(MatchDto.from(matchPlayer.getMatch()));
                        // Add more match-related data as needed
                    }

                    return details;
                })
                .collect(Collectors.toList());
    }

    // Composite DTO for match player details
    public static class MatchPlayerDetails {
        private MatchPlayerDto player;
        private MatchDto match;
        // Add other related DTOs as needed

        public MatchPlayerDto getPlayer() { return player; }
        public void setPlayer(MatchPlayerDto player) { this.player = player; }
        public MatchDto getMatch() { return match; }
        public void setMatch(MatchDto match) { this.match = match; }
        // Add getters and setters for other fields
    }
}