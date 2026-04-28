package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.MatchDto;
import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.services.MatchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
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

}

