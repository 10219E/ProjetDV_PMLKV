package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.MatchCreationDto;
import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.services.MatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/creatematch")
public class MatchCreationController {

    private final MatchService matchService;
    private static final Logger logger = LoggerFactory.getLogger(MatchCreationController.class);

    public MatchCreationController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping(produces = "application/json")
    public ResponseEntity<Map<String,Object>> create(@RequestBody MatchCreationDto dto) {
        logger.info("Create match request received: fieldId={} type={} organiser={}",
                dto != null ? dto.getFieldId() : null,
                dto != null ? dto.getType() : null,
                dto != null ? dto.getOrganiserId() : null);

        if (dto == null || dto.getFieldId() == null || dto.getType() == null || dto.getType().isBlank()
                || dto.getMatchDate() == null || dto.getMatchDate().isBlank()
                || dto.getStartTime() == null || dto.getStartTime().isBlank()
                || dto.getEndTime() == null || dto.getEndTime().isBlank()) {
            logger.warn("Create match failed: missing required fields");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required fields");
        }

        try {
            Match m = new Match();
            m.setType(dto.getType());
            m.setPubStatus(dto.getPubStatus());
            m.setPrivStatus(dto.getPrivStatus());
            m.setMatchDate(LocalDate.parse(dto.getMatchDate()));
            m.setStartTime(LocalTime.parse(dto.getStartTime()));
            m.setEndTime(LocalTime.parse(dto.getEndTime()));
            if (dto.getPricing() != null) m.setPricing(dto.getPricing());

            Field f = new Field();
            f.setFieldId(dto.getFieldId());
            m.setField(f);

            if (dto.getOrganiserId() != null && !dto.getOrganiserId().isBlank()) {
                User u = new User();
                u.setMatricule(dto.getOrganiserId());
                m.setOrganiser(u);
            }

            List<String> invites = dto.getInvites();

            Match saved = matchService.newMatch(m, invites);
            logger.info("Match created id={} field={} type={}", saved.getMatchId(), saved.getField() != null ? saved.getField().getFieldId() : null, saved.getType());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("matchId", saved.getMatchId()));
        } catch (Exception ex) {
            logger.error("Error creating match", ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}

