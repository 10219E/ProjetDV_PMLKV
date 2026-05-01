package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.MatchPlayerDto;
import lu.ephec.backend_projetdv2026.models.MatchPlayers;
import lu.ephec.backend_projetdv2026.services.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/match-players")
public class MatchPlayerController {

        private final MatchService matchService;

        @Autowired
        public MatchPlayerController(MatchService matchService) {
            this.matchService = matchService;
        }

        @PutMapping(value= "/{id}", produces = "application/json")
        public MatchPlayers updateMatchPlayer(@PathVariable Integer id, @RequestBody MatchPlayerDto matchPlayerDto) {
            // Ensure the match ID in the path matches the match ID in the DTO body
            if (!id.equals(matchPlayerDto.getMatch().getMatchId())) {
                throw new IllegalArgumentException("Match ID in path must match matchId in request body");
            }

            // Convert DTO to entity and update
            MatchPlayers updatedMatchPlayer = matchService.updateMatchPlayer(
                    matchPlayerDto.getMatch().getMatchId(),
                    matchPlayerDto.getUser().getMatricule(),
                    matchPlayerDto.getStatus()
            ).orElseThrow(() -> new RuntimeException("MatchPlayer not found for match " + id));

            return updatedMatchPlayer;
        }

        @GetMapping(value = "/mymatches/{userMatricule}", produces = "application/json")
        public Iterable<MatchPlayerDto> getMyMatches(@PathVariable String userMatricule) {
            return matchService.fetchMatchesByUserMatricule(userMatricule).stream()
                    .map(MatchPlayerDto::fromEntity)
                    .toList();
        }
}
