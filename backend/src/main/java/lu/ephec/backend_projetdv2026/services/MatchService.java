package lu.ephec.backend_projetdv2026.services;

import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.models.MatchPlayers;
import lu.ephec.backend_projetdv2026.repo.*;
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
    private final JPAMatchPlayersRepo jpaMatchPlayersRepo;

    // Dependency Injection
    public MatchService(JPAMatchRepo jpaMatchRepo, JPAUserRepo jpaUserRepo, JPAFieldRepo jpaFieldRepo, JPASiteClosureDaysRepo jpaSiteClosureDaysRepo, JPAMatchPlayersRepo jpaMatchPlayersRepo) {
        this.jpaMatchRepo = jpaMatchRepo;
        this.jpaUserRepo = jpaUserRepo;
        this.jpaFieldRepo = jpaFieldRepo;
        this.jpaSiteClosureDaysRepo = jpaSiteClosureDaysRepo;
        this.jpaMatchPlayersRepo = jpaMatchPlayersRepo;
    }


    /// MATCH OPS ///

    //CHECK EXISTS
    public boolean matchExists(Integer matchId) {
        return jpaMatchRepo.existsById(matchId);
    }


    //SET MATCH
    //FOR PUB MATCHES LIST WILL NOT BE PASSED //OVERCHARGE
    @Transactional
    public Match newMatch(Match match) {
        return newMatch(match, null);
    }

    //FOR PRIVATE MATCHES USING THIS
    @Transactional
    public Match newMatch(Match match, List <String> usersToInvite) {
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

            var organizer = jpaUserRepo.findById(match.getOrganiser().getMatricule()).orElseThrow();
            ValidationBoiler.verifyNotAdminUser(organizer.getRole().getId(), organizer.getMatricule());
        }

        //Validate status consistency
        String pubStatus = match.getPubStatus();
        String privStatus = match.getPrivStatus();
        ValidationBoiler.verifyMatchStatusConsistency(match.getType(), pubStatus, privStatus);

        // VALIDATE ALL INVITED USERS EXIST BEFORE CREATING THE MATCH (PRIVATE)
        if (match.getType().equals("private")) {
            // Validate that we have exactly 3 invites (p2, p3, p4 - p1 is organiser)
            int requiredInvites = 3;
            int availableInvites = (usersToInvite != null) ? usersToInvite.size() : 0;

            if (availableInvites < requiredInvites) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Private match requires exactly 3 invited players (p2, p3, p4). " +
                                "Provided: " + availableInvites + ", Required: " + requiredInvites);
            }

            // Validate all invited users exist and are not empty
            for (String userId : usersToInvite) {
                ValidationBoiler.verifyNotEmpty(userId, "User ID in invite list");
                ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
            }
        }

        // SAVE THE MATCH FIRST (to get the auto-generated match_id)
        Match savedMatch = jpaMatchRepo.save(match);

        // NOW INITIALIZE MATCH PLAYERS WITH THE SAVED MATCH (which has an ID)
        initializeMatchPlayers(savedMatch, usersToInvite);

        return savedMatch;
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

    //DELETE -- ONLY FOR TESTS
    @Transactional
    public void deleteMatch(Integer matchId) {
        ValidationBoiler.verifyNotNull(matchId, "Match ID");
        ValidationBoiler.verifyExists(jpaMatchRepo.existsById(matchId), "Match", matchId);

        // Delete associated Match Players first (due to FK constraint)
        jpaMatchPlayersRepo.deleteByMatch_MatchId(matchId);

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

                if (updateData.getType().equals("private") && match.getType().equals("public")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Match can not be migrated from public to private");
                }

                match.setType(updateData.getType());

                // If changing to public, organiser must be null and clean privStatus
                if (updateData.getType().equals("public")) {
                    /// MATCH PLAYERS Reset pending/declined slots to open for public, but keep approved players
                    resetMatchPlayersForPublic(matchId);

                    match.setOrganiser(null);
                    match.setPrivStatus(null);  //CLEANUP OLD STATUS TO AVOID CONSTRAINT if public
                    match.setPubStatus("open"); //Setting Match Open
                } /*else if (updateData.getType().equals("private")) { //CAN NOIT
                    match.setPubStatus(null);   //CLEANUP OLD STATUS TO AVOID CONSTRAINT if private
                }*/ // For now no pub can be changed to private
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

                    var organizer = jpaUserRepo.findById(updateData.getOrganiser().getMatricule()).orElseThrow();
                    ValidationBoiler.verifyNotAdminUser(organizer.getRole().getId(), organizer.getMatricule()); // Check if admin

                    match.setOrganiser(updateData.getOrganiser());
                }
            }

            return jpaMatchRepo.save(match);
        });
    }


    ////MATCH PLAYERS OPS////

    // INITIALIZE ALL 4 PLAYER SLOTS FOR A MATCH
    @Transactional
    protected void initializeMatchPlayers(Match match, List<String> usersToInvite) {
        String[] roles = {"p1", "p2", "p3", "p4"};

        for (int i = 0; i < roles.length; i++) {
            String role = roles[i];
            MatchPlayers player = new MatchPlayers();
            player.setMatch(match);
            player.setPlayerRole(role);

            // For private matches
            if (match.getType().equals("private")) {
                if (role.equals("p1") && match.getOrganiser() != null) {
                    // P1 is the organiser with approved status
                    player.setUser(match.getOrganiser());
                    player.setStatus("approved");
                } else {
                    // Invite the next user in the list to this slot
                    int inviteIndex = i - 1; // Convert role index to invite index (p2->0, p3->1, p4->2)
                    String userIdToInvite = usersToInvite.get(inviteIndex);

                    // Verify user exists
                    var user = jpaUserRepo.findById(userIdToInvite)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "User not found with id: " + userIdToInvite)); //DOUBLE CHECK JUST IN CASE

                    //Check user is not admin
                    ValidationBoiler.verifyNotAdminUser(user.getRole().getId(), user.getMatricule());

                    player.setUser(user);
                    player.setStatus("pending");
                }
            }
            // For public matches: all roles are pending with no user assigned
            else {
                player.setUser(null);
                player.setStatus("pending");
            }

            jpaMatchPlayersRepo.save(player);
        }
    }

    // RESET SPECIFIC SLOTS FOR PUBLIC MATCH
    // Keep approved players, reset declined/pending slots to pending with null user
    @Transactional
    protected void resetMatchPlayersForPublic(Integer matchId) {
        List<MatchPlayers> players = jpaMatchPlayersRepo.findByMatch_MatchId(matchId);

        for (MatchPlayers player : players) {
            // Keep approved players as-is (already booked)
            if (player.getStatus().equals("approved")) {
                continue;
            }

            // Reset declined and pending slots for public filling
            if ((player.getStatus().equals("declined") || player.getStatus().equals("pending")) && player.getUser() != null) {
                player.setUser(null);
                player.setStatus("pending");
            }
        }

        jpaMatchPlayersRepo.saveAll(players);
    }

    // FETCH ALL PLAYERS FOR A MATCH
    public List<MatchPlayers> fetchAllForMatch(Integer matchId) {
        ValidationBoiler.verifyNotNull(matchId, "Match ID");
        ValidationBoiler.verifyExists(jpaMatchRepo.existsById(matchId), "Match", matchId);

        List<MatchPlayers> players = jpaMatchPlayersRepo.findByMatch_MatchId(matchId);
        if (players.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No players found for match: " + matchId);
        }
        return players;
    }

    // UPDATE PLAYER TO MATCH -- autohandling of available role
    // For public matches: fills the first available "pending" slot
    // For private matches: can replace "declined" players
    @Transactional
    public Optional<MatchPlayers> updateMatchPlayer(Integer matchId, String userId, String newStatus) {
        ValidationBoiler.verifyNotNull(matchId, "Match ID");
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyNotEmpty(newStatus, "Status");

        // Verify match exists
        Match match = jpaMatchRepo.findById(matchId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Match not found with id: " + matchId));

        // Verify user exists
        var user = jpaUserRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found with id: " + userId));

        ValidationBoiler.verifyNotAdminUser(user.getRole().getId(), user.getMatricule()); //Check not admin

        // Validate status
        if (!newStatus.matches("^(approved|pending|declined)$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status. Must be approved, pending or declined. Received: " + newStatus);
        }

        // Check user is not already in another role in this match
        Optional<MatchPlayers> existingUserInMatch = jpaMatchPlayersRepo
                .findByMatch_MatchIdAndUser_Matricule(matchId, userId);

        if (existingUserInMatch.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User " + userId + " is already assigned to role " + existingUserInMatch.get().getPlayerRole()
                            + " in match " + matchId);
        }

        List<MatchPlayers> players = jpaMatchPlayersRepo.findByMatch_MatchId(matchId);
        MatchPlayers slotToFill = null;

        // For public matches: find first "pending" slot
        if (match.getType().equals("public")) {
            slotToFill = players.stream()
                    .filter(p -> p.getStatus().equals("pending") && p.getUser() == null)
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "No available slots (pending) in public match " + matchId));
        }
        // For private matches: filling "declined" slots
        else if (match.getType().equals("private")) {
            slotToFill = players.stream()
                    .filter(p -> p.getStatus().equals("declined"))
                    .findFirst()
                    .orElse(null);
        }

        if (slotToFill == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No available slot found in match " + matchId);
        }

        // Assign the user to the slot
        slotToFill.setUser(user);
        slotToFill.setStatus(newStatus);

        return Optional.of(jpaMatchPlayersRepo.save(slotToFill));
    }


}
