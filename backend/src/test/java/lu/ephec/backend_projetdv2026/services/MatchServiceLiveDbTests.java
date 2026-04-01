package lu.ephec.backend_projetdv2026.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.*;
import lu.ephec.backend_projetdv2026.repo.*;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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

    private TestReporter reporter;

    private Match savedPubMatch;
    private Match savedPrivMatch;
    private Field savedField;
    private Site savedSite;
    private User savedOrganiser;

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

    private User createTestOrganiser() {
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
            User organiser = createTestOrganiser();

            // ACT
            Match match = createPrivateMatch(field, organiser);
            Match saved = matchService.newMatch(match);

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

            reporter.publishEntry("info", "Inserted private match matchId=" + saved.getMatchId());

            savedOrganiser = organiser;
            savedPrivMatch = saved;
        }


        @Test
        @Order(3)
        void updateMatchTypeAndOrganiserDB() {
            // ARRANGE
            User newOrganiser = createTestOrganiser();
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

            //CLEANUP
            userService.deleteUser(savedOrganiser.getMatricule()); //CAN DELETE AS MATCH NOT LINKED TO PREVIOUS OR ANY USER
            userService.deleteUser(newOrganiser.getMatricule());

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

            reporter.publishEntry("info", "Deleted match matchId=" + matchId);
        }

        @Test
        @Order(5)
        void fetchByTypeDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);
            User organiser = createTestOrganiser();

            Match publicMatch = createPublicMatch(field);
            Match privateMatch = createPrivateMatch(field, organiser);

            matchService.newMatch(publicMatch);
            matchService.newMatch(privateMatch);

            // ACT
            List<Match> publicMatches = matchService.fetchByType("public");
            List<Match> privateMatches = matchService.fetchByType("private");

            // ASSERT
            assertTrue(matchService.matchExists(publicMatch.getMatchId()));
            assertTrue(matchService.matchExists(privateMatch.getMatchId()));

            assertTrue(publicMatches.stream().allMatch(m -> m.getType().equals("public")));
            assertTrue(privateMatches.stream().allMatch(m -> m.getType().equals("private")));

            reporter.publishEntry("info", "Fetched " + publicMatches.size() + " public and " + privateMatches.size() + " private matches");

            //TO USE IN FURTHER TEST
            savedPrivMatch = privateMatch;
            savedPubMatch = publicMatch;
            savedOrganiser = organiser;
            savedField = field;
            savedSite = site;

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
            User organiser = savedOrganiser;

            // ACT
            List<Match> organiserMatches = matchService.fetchByOrganiser(organiser.getMatricule());

            // ASSERT
            assertNotNull(organiserMatches);
            assertTrue(organiserMatches.size() == 1);
            assertTrue(organiserMatches.stream()
                    .allMatch(m -> m.getOrganiser().getMatricule().equals(organiser.getMatricule())));

            reporter.publishEntry("info", "Fetched " + organiserMatches.size() + " matches for organiser=" + organiser.getMatricule());

            // CLEANUP
            matchService.deleteMatch(savedPrivMatch.getMatchId());
            matchService.deleteMatch(savedPubMatch.getMatchId());
            siteService.deleteSite(savedSite.getSiteId());
            userService.deleteUser(organiser.getMatricule());
        }
    }


    @Nested
    @DisplayName("EXCEPTION - MatchService Tests")
    class ExceptionTests {

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

            reporter.publishEntry("info", "Correctly rejected match with empty type");

            // CLEANUP
            siteService.deleteSite(site.getSiteId());
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

            reporter.publishEntry("info", "Correctly rejected match with invalid type");

            // CLEANUP
            siteService.deleteSite(site.getSiteId());
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

            reporter.publishEntry("info", "Correctly rejected match with past date");

            // CLEANUP
            siteService.deleteSite(site.getSiteId());
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

            reporter.publishEntry("info", "Correctly rejected match with invalid times (end before start)");

            // CLEANUP
            siteService.deleteSite(site.getSiteId());
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

            reporter.publishEntry("info", "Correctly rejected private match without organiser");

            // CLEANUP
            siteService.deleteSite(site.getSiteId());
        }

        @Test
        @Order(7)
        void insertPublicMatchWithOrganiserDB() {
            // ARRANGE
            Site site = createTestSite();
            Field field = createTestField(site);
            User organiser = createTestOrganiser();

            Match match = createPublicMatch(field);
            match.setOrganiser(organiser);

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                matchService.newMatch(match);
            });

            reporter.publishEntry("info", "Correctly rejected public match with organiser");

            // CLEANUP
            siteService.deleteSite(site.getSiteId());
            userService.deleteUser(organiser.getMatricule());
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
    }
}
