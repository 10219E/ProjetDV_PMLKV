package lu.ephec.backend_projetdv2026.services;

import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.repo.JPAMatchRepo;
import lu.ephec.backend_projetdv2026.repo.JPASiteClosureDaysRepo;
import lu.ephec.backend_projetdv2026.repo.JPAUserRepo;
import lu.ephec.backend_projetdv2026.repo.JPAFieldRepo;
import lu.ephec.backend_projetdv2026.services.validation.ValidationBoiler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class MatchService {

    private final JPAMatchRepo jpaMatchRepo;
    private final JPAUserRepo jpaUserRepo;
    private final JPAFieldRepo jpaFieldRepo;
    private final JPASiteClosureDaysRepo jpaSiteClosureDaysRepo;

    // Dependency Injection
    public MatchService(JPAMatchRepo jpaMatchRepo, JPAUserRepo jpaUserRepo, JPAFieldRepo jpaFieldRepo, JPASiteClosureDaysRepo jpaSiteClosureDaysRepo) {
        this.jpaMatchRepo = jpaMatchRepo;
        this.jpaUserRepo = jpaUserRepo;
        this.jpaFieldRepo = jpaFieldRepo;
        this.jpaSiteClosureDaysRepo = jpaSiteClosureDaysRepo;
    }


    //CHECK EXISTS
    public boolean matchExists(Integer matchId) {
        return jpaMatchRepo.existsById(matchId);
    }


    //SET MATCH
    @Transactional
    public Match newMatch(Match match) {
        //Validations
        ValidationBoiler.verifyNotNull(match, "Match");
        ValidationBoiler.verifyNotEmpty(match.getType(), "Match type");
        ValidationBoiler.verifyNotNull(match.getMatchDate(), "Match date");
        ValidationBoiler.verifyNotNull(match.getStartTime(), "Match start time");
        ValidationBoiler.verifyNotNull(match.getEndTime(), "Match end time");
        ValidationBoiler.verifyNotNull(match.getField(), "Match field");

        //Validate match type (private or public)
        ValidationBoiler.verifyValidMatchType(match.getType());

        //Validate times and match date
        ValidationBoiler.verifyDatesValid(LocalDate.now(), match.getMatchDate(), "Match date");
        ValidationBoiler.verifyDatesValid(match.getStartTime(), match.getEndTime(), "Match start/end time");

        //Validate field exists, not on maintenance and get site ID for closure day check
        Field fullField = jpaFieldRepo.findById(match.getField().getFieldId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Field not found with id: " + match.getField().getFieldId()));
        ValidationBoiler.verifyFieldNotUnderMaintenance(match.getMatchDate(), fullField); //Validate field is not under maintenance
        ValidationBoiler.verifyMatchDateNotOnClosureDay(match.getMatchDate(),
                fullField.getSite().getSiteId(), jpaSiteClosureDaysRepo);

        //Validate organiser
        //Validate that organiser is not wrongly assigned (public=null/private=user)
        ValidationBoiler.verifyOrganizerConsistency(match.getType(), match.getOrganiser());
        if (match.getOrganiser() != null && match.getOrganiser().getMatricule() != null) {
            ValidationBoiler.verifyExists(jpaUserRepo.existsById(match.getOrganiser().getMatricule()),
                    "User", match.getOrganiser().getMatricule());
        }

        //Validate status consistency
        String pubStatus = match.getPubStatus();
        String privStatus = match.getPrivStatus();
        ValidationBoiler.verifyMatchStatusConsistency(match.getType(), pubStatus, privStatus);

        return jpaMatchRepo.save(match);
    }

    //GET MATCH BY ID
    public Optional<Match> fetchById(Integer matchId) {
        ValidationBoiler.verifyNotNull(matchId, "Match ID");
        ValidationBoiler.verifyExists(jpaMatchRepo.existsById(matchId), "Match", matchId);
        return jpaMatchRepo.findById(matchId);
    }

    //GET ALL
    public List<Match> fetchAll() {
        return jpaMatchRepo.findAll();
    }

    // GET ALL MATCH FOR TYPE (PUBLIC OR PRIVATE)
    public List<Match> fetchByType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Match type is required");
        }
        if (!type.equals("private") && !type.equals("public")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Match type must be 'private' or 'public'. Received: " + type);
        }
        return jpaMatchRepo.findByType(type);
    }


    //GET MATCH BY TYPE AND STATUS
    public List<Match> fetchMatchesByTypeAndStatus(String type, String status) {
        if (type == null || type.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Match type is required");
        }
        if (!type.equals("private") && !type.equals("public")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Match type must be 'private' or 'public'. Received: " + type);
        }

        if (type.equals("public")) {
            ValidationBoiler.verifyMatchStatusConsistency(type, status, null);
            return jpaMatchRepo.findByTypeAndPubStatus("public", status);
        } else {
            ValidationBoiler.verifyMatchStatusConsistency(type, null, status);
            return jpaMatchRepo.findByTypeAndPrivStatus("private", status);
        }
    }


    //GET MATCH BY DATE RANGE --also valid for startDate=endDate
    public List<Match> fetchByDateRange(LocalDate startDate, LocalDate endDate) {
        ValidationBoiler.verifyNotNull(startDate, "Start date");
        ValidationBoiler.verifyNotNull(endDate, "End date");
        ValidationBoiler.verifyDatesValid(startDate, endDate, "Date range");
        return jpaMatchRepo.findByMatchDateBetween(startDate, endDate);
    }

    //GET BY ORHANISER USER
    public List<Match> fetchByOrganiser(String organiserId) {
        ValidationBoiler.verifyNotEmpty(organiserId, "Organiser ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(organiserId), "User", organiserId);
        return jpaMatchRepo.findByOrganiser_Matricule(organiserId);
    }

    // GET MATCHES ON SPECIFIC SITE
    public List<Match> fetchBySite(Integer siteId) {
        if (siteId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Site ID is required");
        }
        List<Match> matches = jpaMatchRepo.findBySiteId(siteId);
        if (matches.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No matches found for site: " + siteId);
        }
        return matches;
    }

    //DELETE -- ADMIN ONLY
    @Transactional
    public void deleteMatch(Integer matchId) {
        ValidationBoiler.verifyNotNull(matchId, "Match ID");
        ValidationBoiler.verifyExists(jpaMatchRepo.existsById(matchId), "Match", matchId);
        jpaMatchRepo.deleteById(matchId);
    }

    //UPDATE MATCH
    @Transactional
    public Optional<Match> updateMatch(Integer matchId, Match updateData) {
        ValidationBoiler.verifyNotNull(matchId, "Match ID");
        ValidationBoiler.verifyNotNull(updateData, "Update data");
        ValidationBoiler.verifyExists(jpaMatchRepo.existsById(matchId), "Match", matchId);

        return jpaMatchRepo.findById(matchId).map(match -> {

            if (updateData.getType() != null) {
                if (updateData.getType().trim().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Match type is required");
                }
                if (!updateData.getType().equals("private") && !updateData.getType().equals("public")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Match type must be 'private' or 'public'. Received: " + updateData.getType());
                }
                match.setType(updateData.getType());

                // If changing to public, organiser must be null and clean privStatus
                if (updateData.getType().equals("public")) {
                    match.setOrganiser(null);
                    match.setPrivStatus(null);  //CLEANUP OLD STATUS TO AVOID CONSTRAINT if public
                } else if (updateData.getType().equals("private")) {
                    match.setPubStatus(null);   //CLEANUP OLD STATUS TO AVOID CONSTRAINT if private
                }
            }


            // Update times if provided
            if (updateData.getStartTime() != null || updateData.getEndTime() != null) {
                if (updateData.getStartTime() != null) {
                    match.setStartTime(updateData.getStartTime());
                }
                if (updateData.getEndTime() != null) {
                    match.setEndTime(updateData.getEndTime());
                }

                // Validate times after update
                ValidationBoiler.verifyDatesValid(match.getStartTime(), match.getEndTime(), "Match start/end time");
            }

            // Update status if provided
            if (updateData.getPubStatus() != null || updateData.getPrivStatus() != null) {
                // For public matches: pubStatus must be set, privStatus must be null
                if (match.getType().equals("public")) {
                    if (updateData.getPubStatus() != null) {
                        match.setPubStatus(updateData.getPubStatus());
                    }
                    match.setPrivStatus(null);  // CLEAR PREVIOUS STATUS if switch to Public
                    ValidationBoiler.verifyMatchStatusConsistency(match.getType(), match.getPubStatus(), null);
                }
                // For private matches: privStatus must be set, pubStatus must be null
                else if (match.getType().equals("private")) {
                    if (updateData.getPrivStatus() != null) {
                        match.setPrivStatus(updateData.getPrivStatus());
                    }
                    match.setPubStatus(null);  // CLEAR PREVIOUS status if switch private
                    ValidationBoiler.verifyMatchStatusConsistency(match.getType(), null, match.getPrivStatus());
                }
            }

            // Update field if provided
            if (updateData.getField() != null) {
                if (updateData.getField().getFieldId() != null) {
                    ValidationBoiler.verifyExists(jpaFieldRepo.existsById(updateData.getField().getFieldId()),
                            "Field", updateData.getField().getFieldId());
                    ValidationBoiler.verifyFieldNotUnderMaintenance(match.getMatchDate(), updateData.getField()); //Validate field is not under maintenance
                    match.setField(updateData.getField());
                }
            }

            // Update match date if provided -- AFTER FIELD FOR CLOSURE CHECK
            if (updateData.getMatchDate() != null) {
                //Validate field exists and get site ID for closure day check
                Field fullField = jpaFieldRepo.findById(match.getField().getFieldId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "Field not found with id: " + match.getField().getFieldId()));
                ValidationBoiler.verifyFieldNotUnderMaintenance(match.getMatchDate(), fullField); //field not under maintenance
                ValidationBoiler.verifyMatchDateNotOnClosureDay(match.getMatchDate(), //not on closure day
                        fullField.getSite().getSiteId(), jpaSiteClosureDaysRepo);
                ValidationBoiler.verifyDatesValid(LocalDate.now(), match.getMatchDate(), "Match date");
                match.setMatchDate(updateData.getMatchDate());
            }

            // Update organiser if provided - BUT force NULL for public matches
            if (match.getType().equals("public")) {
                match.setOrganiser(null);  //FORCE NULL FOR PUBLIC
            } else if (updateData.getOrganiser() != null) {
                if (updateData.getOrganiser().getMatricule() != null) {
                    ValidationBoiler.verifyExists(jpaUserRepo.existsById(updateData.getOrganiser().getMatricule()),
                            "User", updateData.getOrganiser().getMatricule());
                    match.setOrganiser(updateData.getOrganiser());
                }
            }

            return jpaMatchRepo.save(match);
        });
    }

}
