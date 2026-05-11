package lu.ephec.backend_projetdv2026.services;

import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.models.EnumUserRolesType;
import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.models.MatchPayments;
import lu.ephec.backend_projetdv2026.models.MatchPlayers;
import lu.ephec.backend_projetdv2026.repo.*;
import lu.ephec.backend_projetdv2026.services.validation.ValidationBoiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private final JPAMatchRepo jpaMatchRepo;
    private final JPAUserRepo jpaUserRepo;
    private final JPAFieldRepo jpaFieldRepo;
    private final JPASiteClosureDaysRepo jpaSiteClosureDaysRepo;
    private final JPAMatchPlayersRepo jpaMatchPlayersRepo;
    private final JPAUserSiteRepo jpaUserSiteRepo;
    private final JPAMatchPaymentsRepo jpaMatchPaymentsRepo;
    private final JPAUserAccountsRepo jpaUserAccountsRepo;
    private final JPAUserPenaltiesRepo jpaUserPenaltiesRepo;
    private final Logger logger = LoggerFactory.getLogger(MatchService.class);

    // Dependency Injection
    public MatchService(JPAMatchRepo jpaMatchRepo, JPAUserRepo jpaUserRepo, JPAFieldRepo jpaFieldRepo, JPASiteClosureDaysRepo jpaSiteClosureDaysRepo, JPAMatchPlayersRepo jpaMatchPlayersRepo, JPAUserSiteRepo jpaUserSiteRepo, JPAMatchPaymentsRepo jpaMatchPaymentsRepo, JPAUserAccountsRepo jpaUserAccountsRepo, JPAUserPenaltiesRepo jpaUserPenaltiesRepo) {
        this.jpaMatchRepo = jpaMatchRepo;
        this.jpaUserRepo = jpaUserRepo;
        this.jpaFieldRepo = jpaFieldRepo;
        this.jpaSiteClosureDaysRepo = jpaSiteClosureDaysRepo;
        this.jpaMatchPlayersRepo = jpaMatchPlayersRepo;
        this.jpaUserSiteRepo = jpaUserSiteRepo;
        this.jpaMatchPaymentsRepo = jpaMatchPaymentsRepo;
        this.jpaUserAccountsRepo = jpaUserAccountsRepo;
        this.jpaUserPenaltiesRepo = jpaUserPenaltiesRepo;
    }


    /// MATCH OPS ///

    //CHECK EXISTS
    public boolean matchExists(Integer matchId) {
        return jpaMatchRepo.existsById(matchId);
    }

    //GET MY MATCHES (part of removing business logic of frontend branch 72)
    @Transactional
    public List<Match> fetchMyUpcomingMatches(String userId) {
        // Input validation
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);

        // Get all public and private matches
        List<Match> matchList = new ArrayList<>();

        List<Match> publicMatches = jpaMatchRepo.findByType("public").stream()
                .filter(match ->
                        !"cancelled".equals(match.getPubStatus()) &&
                        !"completed".equals(match.getPubStatus()))
                .toList();

        List<Match> privateMatches = jpaMatchRepo.findByType("private").stream()
                .filter(match ->
                        !"cancelled".equals(match.getPrivStatus()) &&
                        !"completed".equals(match.getPrivStatus()))
                .toList();

        matchList.addAll(publicMatches);
        matchList.addAll(privateMatches);

        // Get matches where user is registered
        List<MatchPlayers> myregistered = jpaMatchPlayersRepo.findByUser_Matricule(userId);

        // Find matches that are both in matchList and in myregistered
        List<Match> mymatches = matchList.stream()
                .filter(match -> myregistered.stream()
                        .anyMatch(mp -> mp.getMatch().getMatchId().equals(match.getMatchId())))
                .toList();

        logger.info("[Service - Match] Found matches where user is registered: " + mymatches.size());

        return mymatches;
    }

    //GET AVAILABLE PUBLIC MATCHES FOR USER (part of removing business logic of frontend branch 72)
    @Transactional
    public List<Match> fetchAvailablePublicMatches(String userId) {
        // Input validation
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);

        var user = jpaUserRepo.findById(userId).orElseThrow();
        boolean hasAllSiteAccess = user.getRole() != null
                && EnumUserRolesType.ALL_SITE_ACCESS.getId().equals(user.getRole().getId());
        Set<Integer> userSiteIds = hasAllSiteAccess
                ? Set.of()
                : jpaUserSiteRepo.findByUser_Matricule(userId).stream()
                .map(link -> link.getSite().getSiteId())
                .collect(Collectors.toSet());

        // Get all public matches that are open or pending
        List<Match> publicMatches = jpaMatchRepo.findByTypeAndPubStatus("public", "open");

        // Get matches where user is registered
        List<MatchPlayers> myregistered = jpaMatchPlayersRepo.findByUser_Matricule(userId);

        // Create a set of match IDs the user is already registered for
        Set<Integer> registeredMatchIds = myregistered.stream()
                .map(mp -> mp.getMatch().getMatchId())
                .collect(Collectors.toSet());

        // Create a set of date-time strings the user is already booked for
        Set<String> userBookedDateTimes = myregistered.stream()
                .map(mp -> mp.getMatch().getMatchDate().toString() + "_" + mp.getMatch().getStartTime().toString())
                .collect(Collectors.toSet());

        // Remove matches that are already full (all match players set to approved)
        List<Match> notFullMatches = publicMatches.stream()
                .filter(match -> {
                    long approvedCount = jpaMatchPlayersRepo.findByMatch_MatchId(match.getMatchId())
                            .stream()
                            .filter(p -> "approved".equals(p.getStatus()))
                            .count();
                    return approvedCount < 4;
                })
                .toList();

        // Remove matches not on my site, except for all_site users
        List<Match> siteFilteredMatches = hasAllSiteAccess
                ? notFullMatches
                : notFullMatches.stream()
                .filter(match -> userSiteIds.contains(match.getField().getSite().getSiteId()))
                .toList();

        // Filter public matches to exclude those the user is already registered for
        List<Match> availableMatches = siteFilteredMatches.stream()
                .filter(match -> !registeredMatchIds.contains(match.getMatchId()))
                .filter(match -> !userBookedDateTimes.contains(match.getMatchDate().toString() + "_" + match.getStartTime().toString()))
                .collect(Collectors.toList());

        logger.info("[Service - Match] Found {} available public matches for user {}", availableMatches.size(), userId);

        return availableMatches;
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
        logger.info("[Service - Match] Creating new match");
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
            String organiserId = match.getOrganiser().getMatricule();
            ValidationBoiler.verifyExists(jpaUserRepo.existsById(organiserId), "User", organiserId);

            // Load full user record from DB and validate fields (including isActive)
            var organizer = jpaUserRepo.findById(organiserId).orElseThrow();
            // Log the loaded organiser and its active flag to help diagnose frontend/DB discrepancies
            ValidationBoiler.verifyUserActive(organizer.getIsActive(), organiserId); // verify if active using DB value
            ValidationBoiler.verifyNotAdminUser(organizer.getRole().getId(), organizer.getMatricule());

            // Ensure the match.organiser contains the loaded user for later use
            match.setOrganiser(organizer);
        }

        //Validate status consistency
        String pubStatus = match.getPubStatus();
        String privStatus = match.getPrivStatus();
        ValidationBoiler.verifyMatchStatusConsistency(match.getType(), pubStatus, privStatus);

        // VALIDATE ALL INVITED USERS EXIST BEFORE CREATING THE MATCH (PRIVATE) + SHOULD NOT BE ADMIN
        if (match.getType().equals("private")) {
            logger.info("[Service - Match] Validating invited users for private match.");
            // Validate that we have exactly 3 invites (p2, p3, p4 - p1 is organiser)
            int requiredInvites = 3;
            int availableInvites = (usersToInvite != null) ? usersToInvite.size() : 0;

            if (availableInvites != requiredInvites) {
                logger.warn("[Service - Match] Invalid number of invites for private match. Provided: {}, Required: {}", availableInvites, requiredInvites);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Private match requires exactly 3 invited players (p2, p3, p4). " +
                                "Provided: " + availableInvites + ", Required: " + requiredInvites);
            }

            // Validate all invited users exist and are not empty
            for (String userId : usersToInvite) {
                ValidationBoiler.verifyNotEmpty(userId, "User ID in invite list");
                ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
                ValidationBoiler.verifyUserActive(jpaUserRepo.findById(userId).orElseThrow().getIsActive(), userId);
                ValidationBoiler.verifyNotAdminUser(jpaUserRepo.findById(userId).orElseThrow().getRole().getId(), userId);
            }
        }

        // VALIDATE ORG AND INVITEES HAVE NO ACTIVE DEBT OR PENALTIES
        if (match.getType().equals("private")) {
            // Check organiser
            if (match.getOrganiser() != null && match.getOrganiser().getMatricule() != null) {
                String organiserId = match.getOrganiser().getMatricule();

                boolean hasActivePenalties = jpaUserPenaltiesRepo.existsActivePenaltyAt(organiserId, LocalDateTime.now());
                ValidationBoiler.verifyNoActivePenalties(hasActivePenalties, organiserId);

                List<MatchPayments> organizerPayments = jpaMatchPaymentsRepo.findByUser_Matricule(organiserId);
                ValidationBoiler.verifyNoOutstandingFinancialObligations(
                        jpaUserAccountsRepo.hasDebt(organiserId),
                        organizerPayments,
                        organiserId
                );
            }

            // Check all invited users
            for (String userId : usersToInvite) {
                if (userId != null) {

                    boolean hasActivePenalties = jpaUserPenaltiesRepo.existsActivePenaltyAt(userId, LocalDateTime.now());
                    ValidationBoiler.verifyNoActivePenalties(hasActivePenalties, userId);

                    List<MatchPayments> userPayments = jpaMatchPaymentsRepo.findByUser_Matricule(userId);
                    ValidationBoiler.verifyNoOutstandingFinancialObligations(
                            jpaUserAccountsRepo.hasDebt(userId),
                            userPayments,
                            userId
                    );
                }
            }
        }

        // SAVE THE MATCH FIRST (to get the auto-generated match_id)
        Match savedMatch = jpaMatchRepo.save(match);

        logger.info("[Service - Match] Match saved with ID: {}", savedMatch.getMatchId());

        // NOW INITIALIZE MATCH PLAYERS WITH THE SAVED MATCH (which has an ID)
        initializeMatchPlayers(savedMatch, usersToInvite);

        logger.info("[Service - Match] Match players initialized for match.");
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

    //GET BY ORGANISER USER
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

        logger.warn("[Service - Match] !!!Deleting match with ID: {} and its players", matchId);
        // Delete associated Match Players first (due to FK constraint)
        jpaMatchPlayersRepo.deleteByMatch_MatchId(matchId);

        jpaMatchRepo.deleteById(matchId);
    }

    //UPDATE MATCH
    @Transactional
    public Optional<Match> updateMatch(Integer matchId, Match updateData) {
        logger.info("[Service - Match] Updating match with ID: {}", matchId);
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
                    logger.info("[Service - Match] Match is now public. Cleaning privStatus.");
                    // MATCH PLAYERS Reset pending/declined slots to open for public, but keep approved players
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
            // Specific case as User Migration
            if (match.getType().equals("public")) {
                match.setOrganiser(null);  //FORCE NULL FOR PUBLIC
            } else if (updateData.getOrganiser() != null) {
                if (updateData.getOrganiser().getMatricule() != null) {
                    ValidationBoiler.verifyExists(jpaUserRepo.existsById(updateData.getOrganiser().getMatricule()),
                            "User", updateData.getOrganiser().getMatricule());

                    // Check if admin
                    var organizer = jpaUserRepo.findById(updateData.getOrganiser().getMatricule()).orElseThrow();
                    ValidationBoiler.verifyNotAdminUser(organizer.getRole().getId(), organizer.getMatricule());

                    //Verify no active penalties
                    String organiserId = organizer.getMatricule();
                    boolean hasActivePenalties = jpaUserPenaltiesRepo.existsActivePenaltyAt(organiserId, LocalDateTime.now());
                    ValidationBoiler.verifyNoActivePenalties(hasActivePenalties, organiserId);

                    //Verify no debt
                    ValidationBoiler.verifyNoOutstandingFinancialObligations(
                            jpaUserAccountsRepo.hasDebt(updateData.getOrganiser().getMatricule()),
                            jpaMatchPaymentsRepo.findByUser_Matricule(updateData.getOrganiser().getMatricule()),
                            updateData.getOrganiser().getMatricule()
                    );


                    match.setOrganiser(updateData.getOrganiser());
                }
            }

            logger.info("[Service - Match] Match updated successfully.");
            return jpaMatchRepo.save(match);
        });
    }


    ////MATCH PLAYERS OPS////

    //GET MATCHES FOR USER
    public List<MatchPlayers> fetchMatchesByUserMatricule(String userMatricule) {
        ValidationBoiler.verifyNotEmpty(userMatricule, "User matricule");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userMatricule), "User", userMatricule);
        return jpaMatchPlayersRepo.findByUser_Matricule(userMatricule);
    }

    // INITIALIZE ALL 4 PLAYER SLOTS FOR A MATCH
    @Transactional
    protected void initializeMatchPlayers(Match match, List<String> usersToInvite) {
        String[] roles = {"p1", "p2", "p3", "p4"};

        logger.info("[Service - Match : Players] Initializing match players for match: {}", match.getMatchId());

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

            logger.info("[Service - Match : Players] Initializing player: {}", player);
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

        logger.info("[Service - Match : Players] Reset match players for public match: {}", matchId);
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

    //DECLINE MATCH PLAYER
    @Transactional
    public MatchPlayers declineMatchPlayer(Integer matchId, String userId) {
        logger.info("[Service - Match : Players] Declining match player for match: {} and user: {}", matchId, userId);
        ValidationBoiler.verifyNotNull(matchId, "Match ID");
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaMatchRepo.existsById(matchId), "Match", matchId);
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);

        // Check user is in this match
        Optional<MatchPlayers> existingUserInMatch = jpaMatchPlayersRepo
                .findByMatch_MatchIdAndUser_Matricule(matchId, userId);

        if (!existingUserInMatch.isPresent()) {
            logger.warn("[Service - Match : Players] User {} is not in match {}", userId, matchId);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "User " + userId + " is not in match " + matchId);
        }

        // Update the existing player's status to declined
        MatchPlayers existingPlayer = existingUserInMatch.get();
        existingPlayer.setStatus("declined");

        logger.info("[Service - Match : Players] Match player declined successfully.");
        return jpaMatchPlayersRepo.save(existingPlayer);
    }

    // UPDATE PLAYER TO MATCH -- autohandling of available role
    // For public matches: fills the first available "pending" slot
    // For private matches: can replace "declined" players
    @Transactional
    public Optional<MatchPlayers> updateMatchPlayer(Integer matchId, String userId, String newStatus) {
        logger.info("[Service - Match : Players] Updating match player for match: {} and user: {}", matchId, userId);
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

        //Verify user is not admin
        ValidationBoiler.verifyNotAdminUser(user.getRole().getId(), user.getMatricule()); //Check not admin

        //1. Verify user has no active penalties
        boolean hasActivePenalties = jpaUserPenaltiesRepo.existsActivePenaltyAt(userId, LocalDateTime.now());
        ValidationBoiler.verifyNoActivePenalties(hasActivePenalties, userId);

        //2. Verify wether user has debt or pending payments
        List<MatchPayments> userPayments = jpaMatchPaymentsRepo.findByUser_Matricule(userId);
        ValidationBoiler.verifyNoOutstandingFinancialObligations(
                jpaUserAccountsRepo.hasDebt(userId),
                userPayments,
                userId
        );

        // Validate status
        if (!newStatus.matches("^(approved|pending|declined)$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status. Must be approved, pending or declined. Received: " + newStatus);
        }

        // Check user is not already in another role in this match
        Optional<MatchPlayers> existingUserInMatch = jpaMatchPlayersRepo
                .findByMatch_MatchIdAndUser_Matricule(matchId, userId);
        
        MatchPlayers slotToFill;

        logger.info("[Service - Match : Players] Processing business logic for player");
        if (existingUserInMatch.isPresent()) {
            // Allow updating if the user has declined status
            if (!existingUserInMatch.get().getStatus().equals("declined")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "User " + userId + " is already assigned to role " + existingUserInMatch.get().getPlayerRole()
                                + " in match " + matchId);
            }
            // If user has declined status, we'll update their existing record instead of creating a new one
            slotToFill = existingUserInMatch.get();
        } else {
            List<MatchPlayers> players = jpaMatchPlayersRepo.findByMatch_MatchId(matchId);
            slotToFill = null;

            // For public matches: find first "pending" slot
            if (match.getType().equals("public")) {
                logger.info("[Service - Match : Players] Checking available slots for public match");
                slotToFill = players.stream()
                        .filter(p -> p.getStatus().equals("pending") && p.getUser() == null)
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                                "No available slots (pending) in public match " + matchId));
            }
            // For private matches: filling "declined" slots
            else if (match.getType().equals("private")) {
                logger.info("[Service - Match : Players] Checking declined slots for private match");
                slotToFill = players.stream()
                        .filter(p -> p.getStatus().equals("declined"))
                        .findFirst()
                        .orElse(null);
            }
        }

        if (slotToFill == null) {
            logger.warn("[Service - Match : Players] No available slot found for match {} and user {}", matchId, userId);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No available slot found in match " + matchId);
        }

        // Assign the user to the slot
        slotToFill.setUser(user);
        slotToFill.setStatus(newStatus);

        logger.info("[Service - Match : Players] Match player updated successfully.");
        return Optional.of(jpaMatchPlayersRepo.save(slotToFill));
    }


}
