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
import jakarta.servlet.http.HttpServletRequest;

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
    public ResponseEntity<Map<String,Object>> create(@RequestBody MatchCreationDto dto, HttpServletRequest request) {
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
            // If the request originates from the frontend path "/create_pmatch" then force
            // public status to NULL and private status to "awaiting" as requested.
            String referer = request != null ? request.getHeader("Referer") : null;
            boolean fromCreatePmatch = referer != null && referer.contains("/create_pmatch");
            if (fromCreatePmatch) {
                // Explicitly treat requests that originated from the create_pmatch frontend route
                // as private matches awaiting confirmation.
                m.setType("private");
                m.setPubStatus(null);
                m.setPrivStatus("awaiting");
            } else {
                // Respect the caller-provided match type when available.
                String dtoType = dto.getType() != null ? dto.getType().trim().toLowerCase() : "";
                if ("private".equals(dtoType)) {
                    m.setType("private");
                    m.setPubStatus(null);
                    // default to 'awaiting' when privStatus is omitted
                    m.setPrivStatus(dto.getPrivStatus() == null || dto.getPrivStatus().isBlank() ? "awaiting" : dto.getPrivStatus());
                } else {
                    // default to public when type is not explicitly 'private'
                    m.setType("public");
                    m.setPrivStatus(null);
                    // default public status to 'open' when omitted
                    m.setPubStatus(dto.getPubStatus() == null || dto.getPubStatus().isBlank() ? "open" : dto.getPubStatus());
                }
            }
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

            // Log an attempt to create the match (helps diagnose when success log doesn't appear)
            logger.info("Attempting to create match: field={} date={} start={} end={} organiser={}",
                    dto.getFieldId(), dto.getMatchDate(), dto.getStartTime(), dto.getEndTime(), dto.getOrganiserId());

            Match saved = matchService.newMatch(m, invites);
            // Log a clear success message including match id, date and organiser matricule
            String organiserMat = (saved.getOrganiser() != null && saved.getOrganiser().getMatricule() != null)
                    ? saved.getOrganiser().getMatricule()
                    : (dto.getOrganiserId() != null ? dto.getOrganiserId() : "_public_");
            logger.info("Match created successfully: id={} date={} by organiser={} field={} type={}",
                    saved.getMatchId(), saved.getMatchDate(), organiserMat,
                    saved.getField() != null ? saved.getField().getFieldId() : null, saved.getType());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("matchId", saved.getMatchId()));
        } catch (Exception ex) {
            logger.error("Error creating match", ex);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }
}

