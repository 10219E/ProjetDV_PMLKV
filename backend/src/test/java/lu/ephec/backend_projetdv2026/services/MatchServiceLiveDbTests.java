package lu.ephec.backend_projetdv2026.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lu.ephec.backend_projetdv2026.models.*;
import lu.ephec.backend_projetdv2026.repo.*;
import com.github.javafaker.Faker;
import org.hibernate.sql.Delete;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class MatchServiceLiveDbTests {

    @PersistenceContext
    private EntityManager em; // Au début de la classe

    @Autowired
    private MatchService matchService;
    @Autowired
    private FieldService fieldService;
    @Autowired
    private SiteService siteService;
    @Autowired
    private UserService userService;
    @Autowired
    private JPASiteClosureDaysRepo jpaSiteClosureDaysRepo;

    @Autowired
    private PaymentService paymentService;


    private TestReporter reporter;

    private Match savedPubMatch;
    private Match savedPrivMatch;
    private Field savedField;
    private Site savedSite;
    //private User savedOrganiser;
    private Map<String, User> usersMatchMap  = new java.util.HashMap<>(); ;

    @BeforeEach
    void initReporter(TestReporter reporter) {
        this.reporter = reporter;
    }

    // ===== HELPER METHODS =====
    private Site createTestSite() {
        Site site = new Site();
        site.setName(Faker.instance().artist().name() + " " + (int)(Math.random() * 10000));
        site.setAddress(Faker.instance().address().streetAddress());
        site.setOpeningTime(LocalTime.of(8, 0));
        site.setClosingTime(LocalTime.of(17, 0));
        site.setIsActive(true);
        return siteService.newSite(site);
    }

    private Field createTestField(Site site) {
        Field field = new Field();
        field.setSite(site);
        field.setIsIndoor(true);
        return fieldService.newField(field);
    }

    private User createTestUser() {
        User user = new User();
        String firstName = Faker.instance().name().firstName();
        String lastName = Faker.instance().name().lastName();
        String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com";
        LocalDate birthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        user.setIsActive(true);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setBirthDate(birthDate);
        user.setRole(em.find(UserRoles.class, (short) 1)); // Role 1 for regular user/organiser
        user.setLevel("confirmé");
        user.setCreated(LocalDateTime.now());
        user.setAuth(null);

        return userService.newUser(user);
    }

    private Match createPublicMatch(Field field) {
        Match match = new Match();
        match.setType("public");
        match.setMatchDate(LocalDate.now().plusDays(5));
        match.setStartTime(LocalTime.of(10, 0));
        match.setEndTime(LocalTime.of(11, 30));
        match.setField(field);
        match.setOrganiser(null);
        match.setPubStatus("open");
        match.setPrivStatus(null);
        return match;
    }

    private Match createPrivateMatch(Field field, User organiser) {
        Match match = new Match();
        match.setType("private");
        match.setMatchDate(LocalDate.now().plusDays(7));
        match.setStartTime(LocalTime.of(14, 0));
        match.setEndTime(LocalTime.of(15, 30));
        match.setField(field);
        match.setOrganiser(organiser);
        match.setPubStatus(null);
        match.setPrivStatus("awaiting");
        return match;
    }

    @Nested
    @DisplayName("CRUD - MatchService Tests")
    class CrudQueryTests {

        ///MATCH OPS///
        @Test
        @Order(1)
        void insertPublicMatchDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);

            // ACT
            Match match = createPublicMatch(field);
            Match saved = matchService.newMatch(match);

            // ASSERT
            assertNotNull(saved);
            assertNotNull(saved.getMatchId());
            assertEquals("public", saved.getType());
            assertNull(saved.getOrganiser());

            Optional<Match> fetchedById = matchService.fetchById(saved.getMatchId());
            assertTrue(fetchedById.isPresent());
            assertEquals("open", fetchedById.get().getPubStatus());

            savedField = field;
            savedSite = site;

            //CLEANUP
            matchService.deleteMatch(saved.getMatchId());

            reporter.publishEntry("info", "Inserted public match matchId=" + saved.getMatchId());
        }

        @Test
        @Order(2)
        void insertPrivateMatchDB() {
            // ARRANGE
            Field field = savedField;
            User organiser = createTestUser();
            User p2 = createTestUser();
            User p3 = createTestUser();
            User p4 = createTestUser();

            List<String> usersToInvite = java.util.List.of(
                    p2.getMatricule(),
                    p3.getMatricule(),
                    p4.getMatricule()
            );

            // ACT
            Match match = createPrivateMatch(field, organiser);
            Match saved = matchService.newMatch(match, usersToInvite);

            // ASSERT
            assertNotNull(saved);
            assertNotNull(saved.getMatchId());
            assertEquals("private", saved.getType());
            assertNotNull(saved.getOrganiser());
            assertEquals(organiser.getMatricule(), saved.getOrganiser().getMatricule());
            assertEquals("awaiting", saved.getPrivStatus());
            assertNull(saved.getPubStatus());

            Optional<Match> fetched = matchService.fetchById(saved.getMatchId());
            assertTrue(fetched.isPresent());
            
            savedPrivMatch = saved;

            // SAVE ALL MATCH PLAYERS -- for further tests
            usersMatchMap.put("p1", organiser);
            usersMatchMap.put("p2", p2);
            usersMatchMap.put("p3", p3);
            usersMatchMap.put("p4", p4);

            reporter.publishEntry("info", "Inserted private match matchId=" + saved.getMatchId());
        }


        @Test
        @Order(3)
        void updateMatchTypeAndOrganiserDB() {
            // ARRANGE
            User newOrganiser = createTestUser();
            Match updateData = new Match();
            updateData.setType("public");
            updateData.setOrganiser(newOrganiser); //SHOULD NOT UPDATE
            updateData.setPubStatus("open"); //SHOULD SET PRIV STATUS TO NULL

            // ACT
            Optional<Match> updatedOpt = matchService.updateMatch(savedPrivMatch.getMatchId(), updateData);

            // ASSERT
            assertTrue(updatedOpt.isPresent());
            Match updated = updatedOpt.get();
            assertEquals("public", updated.getType());
            assertEquals("open", updated.getPubStatus());
            assertNull(updated.getPrivStatus());
            assertNull(updated.getOrganiser()); //Organiser should be null when changing to public// Organiser should be removed when changing to public

            usersMatchMap.put("neworg", newOrganiser); //For further delete

            reporter.publishEntry("info", "Updated match to PUBLIC with organiser that is auto-set to NULL");

        }

        @Test
        @Order(4)
        void deleteMatchDB() {
            // ARRANGE
            Integer matchId = savedPrivMatch.getMatchId();

            // ACT
            matchService.deleteMatch(matchId);

            // ASSERT
            assertFalse(matchService.matchExists(matchId));

            // CLEANUP
            siteService.deleteSite(savedSite.getSiteId()); //ALSO DELETES FIELDS

            // CLEANUP - Delete all users from list
            usersMatchMap.values().forEach(user -> userService.deleteUser(user.getMatricule()));
            usersMatchMap.clear();

            reporter.publishEntry("info", "Deleted match matchId=" + matchId);
        }

        @Test
        @Order(5)
        void fetchByTypeDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);
            User organiser = createTestUser();
            User p2 = createTestUser();
            User p3 = createTestUser();
            User p4 = createTestUser();

            List<String> usersToInvite = List.of(
                    p2.getMatricule(),
                    p3.getMatricule(),
                    p4.getMatricule()
            );

            Match publicMatch = createPublicMatch(field);
            Match privateMatch = createPrivateMatch(field, organiser);

            matchService.newMatch(publicMatch);
            matchService.newMatch(privateMatch, usersToInvite);

            // ACT
            List<Match> publicMatches = matchService.fetchByType("public");
            List<Match> privateMatches = matchService.fetchByType("private");

            // ASSERT
            assertTrue(matchService.matchExists(publicMatch.getMatchId()));
            assertTrue(matchService.matchExists(privateMatch.getMatchId()));

            assertTrue(publicMatches.stream().allMatch(m -> m.getType().equals("public")));
            assertTrue(privateMatches.stream().allMatch(m -> m.getType().equals("private")));


            //TO USE IN FURTHER TEST
            savedPrivMatch = privateMatch;
            savedPubMatch = publicMatch;
            savedField = field;
            savedSite = site;

            // SAVE ALL MATCH PLAYERS
            usersMatchMap.put("p1", organiser);
            usersMatchMap.put("p2", p2);
            usersMatchMap.put("p3", p3);
            usersMatchMap.put("p4", p4);
            

            reporter.publishEntry("info", "Fetched " + publicMatches.size() + " public and " + privateMatches.size() + " private matches");

        }

        @Test
        @Order(6)
        void fetchMatchesByTypeAndStatusDB() {
            // ACT
            List<Match> openPubMatches = matchService.fetchMatchesByTypeAndStatus("public", "open");
            List<Match> awaitingPrivMatches = matchService.fetchMatchesByTypeAndStatus("private", "awaiting");

            // ASSERT
            assertTrue(openPubMatches.stream().allMatch(m -> m.getPubStatus().equals("open")));
            assertTrue(awaitingPrivMatches.stream().allMatch(m -> m.getPrivStatus().equals("awaiting")));

            reporter.publishEntry("info", "Fetched Open Public Matches: " + openPubMatches.size() + " and " + awaitingPrivMatches.size() + " Awaiting Private Matches");

        }

        @Test
        @Order(7)
        void fetchByDateRangeDB() {

            // ACT
            LocalDate startDate = LocalDate.now().plusDays(2);
            LocalDate endDate = LocalDate.now().plusDays(10);
            List<Match> matchesInRange = matchService.fetchByDateRange(startDate, endDate);

            // ASSERT
            assertNotNull(matchesInRange);
            assertTrue(matchesInRange.stream()
                    .allMatch(m -> !m.getMatchDate().isBefore(startDate) && !m.getMatchDate().isAfter(endDate)));

            reporter.publishEntry("info", "Fetched " + matchesInRange.size() + " matches in date range");
        }

        @Test
        @Order(8)
        void fetchByOrganiserDB() {
            // ARRANGE
            User organiser = usersMatchMap.get("p1");

            // ACT
            List<Match> organiserMatches = matchService.fetchByOrganiser(organiser.getMatricule());

            // ASSERT
            assertNotNull(organiserMatches);
            assertTrue(organiserMatches.size() == 1);
            assertTrue(organiserMatches.stream()
                    .allMatch(m -> m.getOrganiser().getMatricule().equals(organiser.getMatricule())));

            // CLEANUP
            matchService.deleteMatch(savedPrivMatch.getMatchId());
            matchService.deleteMatch(savedPubMatch.getMatchId());
            siteService.deleteSite(savedSite.getSiteId());

            //Delete all players from list
            usersMatchMap.values().forEach(user -> userService.deleteUser(user.getMatricule()));
            usersMatchMap.clear();

            reporter.publishEntry("info", "Fetched " + organiserMatches.size() + " matches for organiser=" + organiser.getMatricule());
        }

        /// MATCH PLAYER OPS TESTS///

        @Test
        @Order(9)
        void fetchAllPlayersForPublicMatchDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);
            Match pubMatch = createPublicMatch(field);
            Match saved = matchService.newMatch(pubMatch);

            // ACT
            List<MatchPlayers> players = matchService.fetchAllForMatch(saved.getMatchId());

            // ASSERT
            assertNotNull(players);
            assertEquals(4, players.size());
            assertTrue(players.stream().allMatch(p -> p.getStatus().equals("pending")));
            assertTrue(players.stream().allMatch(p -> p.getUser() == null));
            assertTrue(players.stream().map(MatchPlayers::getPlayerRole)
                    .allMatch(role -> role.matches("^p[1-4]$")));

            // CLEANUP
            matchService.deleteMatch(saved.getMatchId());
            siteService.deleteSite(site.getSiteId());

            reporter.publishEntry("info", "Fetched 4 pending players for public match");
        }

        @Test
        @Order(10)
        void fetchAllPlayersForPrivateMatchDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);
            User organiser = createTestUser();
            User p2 = createTestUser();
            User p3 = createTestUser();
            User p4 = createTestUser();

            List<String> usersToInvite = List.of(p2.getMatricule(), p3.getMatricule(), p4.getMatricule());
            Match privMatch = createPrivateMatch(field, organiser);
            Match saved = matchService.newMatch(privMatch, usersToInvite);

            // ACT
            List<MatchPlayers> players = matchService.fetchAllForMatch(saved.getMatchId());

            // ASSERT
            assertNotNull(players);
            assertEquals(4, players.size());

            // Verify p1 (organiser) is approved
            MatchPlayers p1 = players.stream()
                    .filter(p -> p.getPlayerRole().equals("p1"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(p1);
            assertEquals("approved", p1.getStatus());
            assertEquals(organiser.getMatricule(), p1.getUser().getMatricule());

            // Verify p2, p3, p4 are pending
            long invitedCount = players.stream()
                    .filter(p -> p.getStatus().equals("pending"))
                    .count();
            assertEquals(3, invitedCount);

            // CLEANUP
            matchService.deleteMatch(saved.getMatchId());
            siteService.deleteSite(site.getSiteId());
            userService.deleteUser(organiser.getMatricule());
            userService.deleteUser(p2.getMatricule());
            userService.deleteUser(p3.getMatricule());
            userService.deleteUser(p4.getMatricule());

            reporter.publishEntry("info", "Fetched 4 players for private match (1 approved organiser, 3 invited)");
        }

        @Test
        @Order(11)
        void addPlayersToPublicMatchDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);
            Match pubMatch = createPublicMatch(field);
            Match saved = matchService.newMatch(pubMatch);

            User player1 = createTestUser();
            User player2 = createTestUser();
            User player3 = createTestUser();

            // ACT
            Optional<MatchPlayers> updated1 = matchService.updateMatchPlayer(
                    saved.getMatchId(), player1.getMatricule(), "approved");
            Optional<MatchPlayers> updated2 = matchService.updateMatchPlayer(
                    saved.getMatchId(), player2.getMatricule(), "approved");
            Optional<MatchPlayers> updated3 = matchService.updateMatchPlayer(
                    saved.getMatchId(), player3.getMatricule(), "pending");

            // ASSERT
            assertTrue(updated1.isPresent());
            assertTrue(updated2.isPresent());
            assertTrue(updated3.isPresent());
            assertEquals("approved", updated1.get().getStatus());
            assertEquals("approved", updated2.get().getStatus());
            assertEquals("pending", updated3.get().getStatus());

            List<MatchPlayers> allPlayers = matchService.fetchAllForMatch(saved.getMatchId());
            long approvedCount = allPlayers.stream()
                    .filter(p -> p.getStatus().equals("approved") && p.getUser() != null)
                    .count();
            assertEquals(2, approvedCount);

            // CLEANUP
            matchService.deleteMatch(saved.getMatchId());
            siteService.deleteSite(site.getSiteId());
            userService.deleteUser(player1.getMatricule());
            userService.deleteUser(player2.getMatricule());
            userService.deleteUser(player3.getMatricule());

            reporter.publishEntry("info", "Added 3 players to public match");
        }
    }

    @Nested
    @DisplayName("EXCEPTION - MatchService Tests")
    class ExceptionTests {


        ///MATCH OPS EXCEPTION///
        @Test
        @Order(1)
        void insertMatchWithEmptyTypeDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);

            Match match = createPublicMatch(field);
            match.setType("");

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.newMatch(match);
            });

            // CLEANUP
            siteService.deleteSite(site.getSiteId());

            reporter.publishEntry("info", "Correctly rejected match with empty type");
        }

        @Test
        @Order(2)
        void insertMatchWithInvalidTypeDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);

            Match match = createPublicMatch(field);
            match.setType("invalid");

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.newMatch(match);
            });

            // CLEANUP
            siteService.deleteSite(site.getSiteId());

            reporter.publishEntry("info", "Correctly rejected match with invalid type");
        }

        @Test
        @Order(3)
        void insertMatchWithPastDateDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);

            Match match = createPublicMatch(field);
            match.setMatchDate(LocalDate.now().minusDays(1));

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.newMatch(match);
            });

            // CLEANUP
            siteService.deleteSite(site.getSiteId());

            reporter.publishEntry("info", "Correctly rejected match with past date");
        }

        @Test
        @Order(4)
        void insertMatchWithInvalidTimesDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);

            Match match = createPublicMatch(field);
            match.setStartTime(LocalTime.of(15, 0));
            match.setEndTime(LocalTime.of(14, 0)); // End before start

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.newMatch(match);
            });

            // CLEANUP
            siteService.deleteSite(site.getSiteId());

            reporter.publishEntry("info", "Correctly rejected match with invalid times (end before start)");
        }

        @Test
        @Order(5)
        void insertMatchWithNonExistentFieldDB() {
            // ARRANGE
            Match match = createPublicMatch(null);
            Field invalidField = new Field();
            invalidField.setFieldId(999999);
            match.setField(invalidField);

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.newMatch(match);
            });

            reporter.publishEntry("info", "Correctly rejected match with non-existent field");
        }

        @Test
        @Order(6)
        void insertPrivateMatchWithoutOrganiserDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);

            Match match = createPrivateMatch(field, null);

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.newMatch(match);
            });

            // CLEANUP
            siteService.deleteSite(site.getSiteId());

            reporter.publishEntry("info", "Correctly rejected private match without organiser");
        }

        @Test
        @Order(7)
        void insertPublicMatchWithOrganiserDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);
            User organiser = createTestUser();

            Match match = createPublicMatch(field);
            match.setOrganiser(organiser);

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.newMatch(match);
            });

            // CLEANUP
            siteService.deleteSite(site.getSiteId());
            userService.deleteUser(organiser.getMatricule());

            reporter.publishEntry("info", "Correctly rejected public match with organiser");
        }

        @Test
        @Order(8)
        void fetchByInvalidTypeDB() {
            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.fetchByType("invalid");
            });

            reporter.publishEntry("info", "Correctly rejected fetch with invalid type");
        }

        @Test
        @Order(9)
        void fetchByInvalidSiteDB() {
            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.fetchBySite(999999);
            });

            reporter.publishEntry("info", "Correctly rejected fetch for non-existent site");
        }

        @Test
        @Order(10)
        void insertMatchOnSiteClosureDayDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);

            // Create a closure day for the site
            LocalDate closureDate = LocalDate.now().plusDays(5);
            SiteClosureDays closure = new SiteClosureDays();
            closure.setSiteId(site.getSiteId());
            closure.setClosureDate(closureDate);
            closure.setReason("Maintenance");
            jpaSiteClosureDaysRepo.save(closure); // Save closure day

            Match match = createPublicMatch(field);
            match.setMatchDate(closureDate); // Try to create match on closure day

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.newMatch(match);
            });

            // CLEANUP
            siteService.deleteSite(site.getSiteId());

            reporter.publishEntry("info", "Correctly rejected match on site closure day: " + closureDate);

        }

        @Test
        @Order(11)
        void insertMatchOnFieldUnderMaintenanceDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);

            // Set maintenance period
            LocalDate maintenanceStart = LocalDate.now().plusDays(5);
            LocalDate maintenanceEnd = LocalDate.now().plusDays(10);
            field.setMaintenanceFromDate(maintenanceStart);
            field.setMaintenanceToDate(maintenanceEnd);
            fieldService.updateField(field.getFieldId(), field);

            Match match = createPublicMatch(field);
            match.setMatchDate(maintenanceStart.plusDays(2)); // Try to create match during maintenance

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.newMatch(match);
            });

            // CLEANUP
            siteService.deleteSite(site.getSiteId());

            reporter.publishEntry("info", "Correctly rejected match on field under maintenance from " + maintenanceStart + " to " + maintenanceEnd);
        }

        @Test
        @Order(12)
        void insertPrivateMatchAsAdminOrganizerDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);

            // Create admin user (role 7 = Site Admin)
            User adminUser = new User();
            String firstName = Faker.instance().name().firstName();
            String lastName = Faker.instance().name().lastName();
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@admin.com";
            LocalDate birthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            adminUser.setIsActive(true);
            adminUser.setFirstName(firstName);
            adminUser.setLastName(lastName);
            adminUser.setEmail(email);
            adminUser.setBirthDate(birthDate);
            adminUser.setRole(em.find(UserRoles.class, (short) 9)); // Role 9 = Super Admin
            adminUser.setCreated(LocalDateTime.now());
            adminUser.setAuth(null);

            User savedAdmin = userService.newUser(adminUser);

            // Create 3 regular invited users
            User p2 = createTestUser();
            User p3 = createTestUser();
            User p4 = createTestUser();

            List<String> usersToInvite = List.of(p2.getMatricule(), p3.getMatricule(), p4.getMatricule());

            Match match = createPrivateMatch(field, savedAdmin);

            // ACT & ASSERT - Admin cannot create match as organiser
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.newMatch(match, usersToInvite);
            });

            // CLEANUP
            siteService.deleteSite(site.getSiteId());
            userService.deleteUser(savedAdmin.getMatricule());
            userService.deleteUser(p2.getMatricule());
            userService.deleteUser(p3.getMatricule());
            userService.deleteUser(p4.getMatricule());

            reporter.publishEntry("info", "Correctly rejected admin user from creating match as organiser");
        }

        @Test
        @Order(13)
        void insertPrivateMatchWithOrganizerHasDebtDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);

            User organiser = createTestUser();
            User p2 = createTestUser();
            User p3 = createTestUser();
            User p4 = createTestUser();

            // Give organiser debt status
            paymentService.updateAccountStatus(organiser.getMatricule(), "debt", -50.00);

            List<String> usersToInvite = List.of(p2.getMatricule(), p3.getMatricule(), p4.getMatricule());
            Match match = createPrivateMatch(field, organiser);

            // ACT & ASSERT - Organiser with debt cannot create match
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.newMatch(match, usersToInvite);
            });

            Optional<Double> balance = paymentService.fetchUserBalance(organiser.getMatricule());
            assertEquals(-50.0, balance.get());  // ✓ Extracts value from Optional

            // CLEANUP
            siteService.deleteSite(site.getSiteId());
            userService.deleteUser(organiser.getMatricule());
            userService.deleteUser(p2.getMatricule());
            userService.deleteUser(p3.getMatricule());
            userService.deleteUser(p4.getMatricule());

            reporter.publishEntry("info", "Correctly rejected organiser with debt from creating match");
        }

        /// MATCH PLAYER OPS EXCEPTION TESTS///
        @Test
        @Order(14)
        void updateMatchPlayerWhenAllSlotsFullDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);
            Match pubMatch = createPublicMatch(field);
            Match saved = matchService.newMatch(pubMatch);

            User player1 = createTestUser();
            User player2 = createTestUser();
            User player3 = createTestUser();
            User player4 = createTestUser();
            User player5 = createTestUser(); // 5th player - should fail

            // Fill all 4 slots
            matchService.updateMatchPlayer(saved.getMatchId(), player1.getMatricule(), "approved");
            matchService.updateMatchPlayer(saved.getMatchId(), player2.getMatricule(), "approved");
            matchService.updateMatchPlayer(saved.getMatchId(), player3.getMatricule(), "approved");
            matchService.updateMatchPlayer(saved.getMatchId(), player4.getMatricule(), "approved");

            // ACT & ASSERT - Try to add 5th player
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.updateMatchPlayer(saved.getMatchId(), player5.getMatricule(), "approved");
            });

            // CLEANUP
            matchService.deleteMatch(saved.getMatchId());
            siteService.deleteSite(site.getSiteId());
            userService.deleteUser(player1.getMatricule());
            userService.deleteUser(player2.getMatricule());
            userService.deleteUser(player3.getMatricule());
            userService.deleteUser(player4.getMatricule());
            userService.deleteUser(player5.getMatricule());

            reporter.publishEntry("info", "Correctly rejected adding player to full match (all 4 slots occupied)");
        }

        @Test
        @Order(15)
        void joinMatchWithPlayerHasDebtDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);
            Match pubMatch = createPublicMatch(field);
            Match saved = matchService.newMatch(pubMatch);

            User player = createTestUser();

            // Give player debt status
            paymentService.updateAccountStatus(player.getMatricule(), "debt", -50.00);

            // ACT & ASSERT - Player with debt cannot join match
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.updateMatchPlayer(saved.getMatchId(), player.getMatricule(), "approved");
            });

            Optional<Double> balance = paymentService.fetchUserBalance(player.getMatricule());
            assertEquals(-50.0, balance.get());  // ✓ Extracts value from Optional

            // CLEANUP
            matchService.deleteMatch(saved.getMatchId());
            siteService.deleteSite(site.getSiteId());
            userService.deleteUser(player.getMatricule());

            reporter.publishEntry("info", "Correctly rejected player with debt from joining match");
        }

    }
}
