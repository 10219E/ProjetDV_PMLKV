package lu.ephec.backend_projetdv2026.services.validation;

import com.github.javafaker.Faker;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lu.ephec.backend_projetdv2026.models.*;
import lu.ephec.backend_projetdv2026.repo.*;
import lu.ephec.backend_projetdv2026.services.FieldService;
import lu.ephec.backend_projetdv2026.services.MatchService;
import lu.ephec.backend_projetdv2026.services.MigrateUserDESTRUCTIVE;
import lu.ephec.backend_projetdv2026.services.UserService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.client.match.JsonPathRequestMatchers;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class UserMigrationLiveDbTests {

    @Autowired
    private UserService userService;

    @Autowired
    private JPAUserRepo jpaUserRepo;

    @Autowired
    private JPAUserPenaltiesRepo jpaUserPenaltiesRepo;

    @Autowired
    private MigrateUserDESTRUCTIVE migrateUserDESTRUCTIVE;

    @PersistenceContext
    private EntityManager em;

    private TestReporter reporter;

    private User savedUser; //TO REUSE USER OBJECT
    @Autowired
    private JPAFieldRepo jPAFieldRepo;

    @Autowired
    private JPAMatchRepo jpaMatchRepo;

    @Autowired
    private JPAMatchPlayersRepo jpaMatchPlayersRepo;
    @Autowired
    private MatchService matchService;
    @Autowired
    private FieldService fieldService;

    @BeforeEach
    void initReporter(TestReporter reporter) {
        this.reporter = reporter;
    }

    ///////CRUD MIGRATION TESTS///////
    @Nested
    @DisplayName("MIGRATION - CRUD Tests")
    class UserMigrationTests {

        @Test
        @Order(1)
        void migrateInviteToSubscribedDB() {
            // ARRANGE
            String firstName = Faker.instance().name().firstName();
            String lastName = Faker.instance().name().lastName();
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@migrate.com";
            LocalDate birthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            // Create Invite user (L prefix, role 0)
            User u = new User();
            u.setIsActive(true);
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setEmail(email);
            u.setBirthDate(birthDate);
            u.setRole(em.find(UserRoles.class, (short) 0)); // Invite
            u.setLevel("débutant");
            u.setCreated(LocalDateTime.now());
            u.setAuth(null);

            //CALL
            User savedInvite = userService.newUser(u);

            // ACT - Migrate L → S (Invite → Subscribed)
            User migratedUser = migrateUserDESTRUCTIVE.migrateUserRole(savedInvite.getMatricule(), (short) 1);

            // ASSERT
            assertNotNull(migratedUser);
            assertTrue(migratedUser.getMatricule().startsWith("S"), "Migrated user should have S prefix");
            assertNotEquals(savedInvite.getMatricule(), migratedUser.getMatricule(), "Matricule should have changed");
            assertEquals(firstName, migratedUser.getFirstName(), "First name should be preserved");
            assertEquals(email, migratedUser.getEmail(), "Email should be preserved");
            assertEquals((short) 1, migratedUser.getRole().getId(), "Role should be 1 (Subscribed)");
            assertFalse(jpaUserRepo.existsById(savedInvite.getMatricule()), "Old matricule should be deleted");
            assertTrue(jpaUserRepo.existsById(migratedUser.getMatricule()), "New matricule should exist");

            savedUser = migratedUser;

            reporter.publishEntry("info", "Migrated L → S: " + savedInvite.getMatricule() + " → " + migratedUser.getMatricule());
        }

        @Test
        @Order(2)
        void migrateSubscribedTouB() {
            // ARRANGE
            String oldMatricule = savedUser.getMatricule();

            // ACT - Migrate S → L (Subscribed → Invite)
            User migratedUser = migrateUserDESTRUCTIVE.migrateUserRole(oldMatricule, (short) 0);

            // ASSERT
            assertNotNull(migratedUser);
            assertTrue(migratedUser.getMatricule().startsWith("L"), "Migrated user should have L prefix");
            assertEquals((short) 0, migratedUser.getRole().getId(), "Role should be 0 (Invite)");
            assertFalse(jpaUserRepo.existsById(oldMatricule), "Old matricule should be deleted");
            assertTrue(jpaUserRepo.existsById(migratedUser.getMatricule()), "New matricule should exist");

            savedUser = migratedUser;

            reporter.publishEntry("info", "Migrated S → L: " + oldMatricule + " → " + migratedUser.getMatricule());
        }

        @Test
        @Order(3)
        void migrateUserWithInactivePenaltiesDB() {
            // ARRANGE
            // Add multiple INACTIVE penalties
            UserPenalties penalty1 = new UserPenalties();
            penalty1.setUser(savedUser);
            penalty1.setReason("unpaid_balance");
            penalty1.setStartDate(LocalDateTime.now().minusDays(90));
            penalty1.setEndDate(LocalDateTime.now().minusDays(60));
            penalty1.setIsActive(false);
            penalty1.setDescription("Old penalty 1");
            penalty1.setMatchId(null); //NULL TO AVOID CONSTRAINT
            jpaUserPenaltiesRepo.save(penalty1);

            UserPenalties penalty2 = new UserPenalties();
            penalty2.setUser(savedUser);
            penalty2.setReason("no_show");
            penalty2.setStartDate(LocalDateTime.now().minusDays(60));
            penalty2.setEndDate(LocalDateTime.now().minusDays(30));
            penalty2.setIsActive(false);
            penalty2.setDescription("Old penalty 2");
            penalty2.setMatchId(null);
            jpaUserPenaltiesRepo.save(penalty2);

            UserPenalties penalty3 = new UserPenalties();
            penalty3.setUser(savedUser);
            penalty3.setReason("insufficient_players");
            penalty3.setStartDate(LocalDateTime.now().minusDays(30));
            penalty3.setEndDate(LocalDateTime.now().minusDays(1));
            penalty3.setIsActive(false);
            penalty3.setDescription("Old penalty 3");
            penalty3.setMatchId(null);
            jpaUserPenaltiesRepo.save(penalty3);

            // ACT - Migrate L → S again (Invite → Subscribed)
            User migratedUser = migrateUserDESTRUCTIVE.migrateUserRole(savedUser.getMatricule(), (short) 1);

            // ASSERT
            assertNotNull(migratedUser);
            assertTrue(migratedUser.getMatricule().startsWith("S"), "Migrated user should have S prefix");

            // Check all penalties were migrated
            List<UserPenalties> migratedPenalties = jpaUserPenaltiesRepo.findByUserMatriculeWithUser(migratedUser.getMatricule());
            assertEquals(3, migratedPenalties.size(), "All 3 penalties should be migrated");

            assertTrue(migratedPenalties.stream().anyMatch(p -> p.getReason().equals("unpaid_balance")), "Penalty 1 should exist");
            assertTrue(migratedPenalties.stream().anyMatch(p -> p.getReason().equals("no_show")), "Penalty 2 should exist");
            assertTrue(migratedPenalties.stream().anyMatch(p -> p.getReason().equals("insufficient_players")), "Penalty 3 should exist");

            // CLEANUP
            userService.deleteAllPenaltiesForUser(migratedUser.getMatricule());
            userService.deleteUser(migratedUser.getMatricule());

            reporter.publishEntry("info", "Migrated user with 3 inactive penalties: " + savedUser.getMatricule() + " → " + migratedUser.getMatricule());

        }

        @Test
        @Order(4)
        void migrateToAdminDB() {
            // ARRANGE
            String firstName = Faker.instance().name().firstName();
            String lastName = Faker.instance().name().lastName();
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@admin.com";
            LocalDate birthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            User admin = new User();
            admin.setIsActive(true);
            admin.setFirstName(firstName);
            admin.setLastName(lastName);
            admin.setEmail(email);
            admin.setBirthDate(birthDate);
            admin.setRole(em.find(UserRoles.class, (short) 7)); // Site Admin (M)
            //admin.setLevel("confirmé"); //WILL BE SET TO DEFAULT null
            admin.setCreated(LocalDateTime.now());
            admin.setAuth(null);

            User savedAdmin = userService.newUser(admin);

            // ACT - Migrate M(7) → A(9) (Site Admin → Super Admin)
            User migratedAdmin = migrateUserDESTRUCTIVE.migrateUserRole(savedAdmin.getMatricule(), (short) 9);

            // ASSERT
            assertNotNull(migratedAdmin);
            assertTrue(migratedAdmin.getMatricule().startsWith("A"), "Migrated admin should have A prefix");
            assertNotEquals(savedAdmin.getMatricule(), migratedAdmin.getMatricule(), "Matricule should have changed");
            assertEquals((short) 9, migratedAdmin.getRole().getId(), "Role should be 9 (Super Admin)");
            assertEquals(email, migratedAdmin.getEmail(), "Email should be preserved");
            assertFalse(jpaUserRepo.existsById(savedAdmin.getMatricule()), "Old matricule should be deleted");
            assertTrue(jpaUserRepo.existsById(migratedAdmin.getMatricule()), "New matricule should exist");

            // CLEANUP
            userService.deleteUser(migratedAdmin.getMatricule());

            reporter.publishEntry("info", "Admin migration successful: " + savedAdmin.getMatricule() + " → " + migratedAdmin.getMatricule());
        }

        @Test
        @Order(5)
        void migrateUserWithMatchAndPlayerHistory() {
            // ARRANGE - Create organiser user to migrate
            String firstName = Faker.instance().name().firstName();
            String lastName = Faker.instance().name().lastName();
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@migrate.com";
            LocalDate birthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            User organiser = new User();
            organiser.setIsActive(true);
            organiser.setFirstName(firstName);
            organiser.setLastName(lastName);
            organiser.setEmail(email);
            organiser.setBirthDate(birthDate);
            organiser.setRole(em.find(UserRoles.class, (short) 1)); // Subscribed
            organiser.setLevel("confirmé");
            organiser.setCreated(LocalDateTime.now());
            organiser.setAuth(null);
            User oldOrganiser = userService.newUser(organiser);

            // Create 3 users for invitation list
            User u1 = new User();
            u1.setIsActive(true);
            u1.setFirstName(Faker.instance().name().firstName());
            u1.setLastName(Faker.instance().name().lastName());
            u1.setEmail(Faker.instance().internet().emailAddress());
            u1.setBirthDate(Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            u1.setRole(em.find(UserRoles.class, (short) 1));
            u1.setLevel("débutant");
            u1.setCreated(LocalDateTime.now());
            u1.setAuth(null);
            u1 = userService.newUser(u1);

            User u2 = new User();
            u2.setIsActive(true);
            u2.setFirstName(Faker.instance().name().firstName());
            u2.setLastName(Faker.instance().name().lastName());
            u2.setEmail(Faker.instance().internet().emailAddress());
            u2.setBirthDate(Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            u2.setRole(em.find(UserRoles.class, (short) 1));
            u2.setLevel("averti");
            u2.setCreated(LocalDateTime.now());
            u2.setAuth(null);
            u2 = userService.newUser(u2);

            User u3 = new User();
            u3.setIsActive(true);
            u3.setFirstName(Faker.instance().name().firstName());
            u3.setLastName(Faker.instance().name().lastName());
            u3.setEmail(Faker.instance().internet().emailAddress());
            u3.setBirthDate(Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            u3.setRole(em.find(UserRoles.class, (short) 1));
            u3.setLevel("confirmé");
            u3.setCreated(LocalDateTime.now());
            u3.setAuth(null);
            u3 = userService.newUser(u3);

            // Get any field from DB
            Field field = jPAFieldRepo.findAll().stream().findAny().orElseThrow(() -> new RuntimeException("No fields found in DB"));

            // Create private match with organiser and 3 u users
            Match m = new Match();
            m.setField(field);
            m.setType("private");
            m.setPrivStatus("confirmed");
            m.setMatchDate(LocalDate.now().plusDays(7));
            m.setStartTime(LocalTime.of(14, 0));
            m.setEndTime(LocalTime.of(15, 0));
            m.setOrganiser(oldOrganiser);
            m.setMinPlayers(2);
            m.setMaxPlayers(4);
            m.setPricing(50);

            List<String> uMatricules = List.of(u1.getMatricule(), u2.getMatricule(), u3.getMatricule());
            Match savedMatch = matchService.newMatch(m, uMatricules);

            // ACT - Migrate organiser user
            User migratedOrganiser = migrateUserDESTRUCTIVE.migrateUserRole(oldOrganiser.getMatricule(), (short) 0);

            // ASSERT
            assertNotNull(migratedOrganiser);
            assertTrue(migratedOrganiser.getMatricule().startsWith("L"), "Migrated user should have L prefix");

            // Check match organiser was updated
            Match updatedMatch = jpaMatchRepo.findById(savedMatch.getMatchId()).orElse(null);
            assertNotNull(updatedMatch);
            assertEquals(migratedOrganiser.getMatricule(), updatedMatch.getOrganiser().getMatricule(),
                    "Match organiser should be updated to migrated user");

            // Check match players were updated
            List<MatchPlayers> playersInMatch = jpaMatchPlayersRepo.findByMatch_MatchId(savedMatch.getMatchId());
            MatchPlayers p1 = playersInMatch.stream().filter(p -> p.getPlayerRole().equals("p1")).findFirst().orElse(null);
            assertNotNull(p1, "Player p1 should exist");
            assertEquals(migratedOrganiser.getMatricule(), p1.getUser().getMatricule(),
                    "Player p1 should be updated to migrated organiser");

            // CLEANUP
            matchService.deleteMatch(savedMatch.getMatchId());
            userService.deleteUser(migratedOrganiser.getMatricule());
            userService.deleteUser(u1.getMatricule());
            userService.deleteUser(u2.getMatricule());
            userService.deleteUser(u3.getMatricule());

            reporter.publishEntry("info", "Match and player history migration test passed: " + oldOrganiser.getMatricule() + " → " + migratedOrganiser.getMatricule());
        }

    }

    ///////EXCEPTION MIGRATION TESTS///////
    @Nested
    @DisplayName("MIGRATION - EXCEPTIONS Tests")
    class NegativeUserMigrationTests {

        @Test
        @Order(1)
        void migrateSameRoleDB() {
            // ARRANGE
            String firstName = Faker.instance().name().firstName();
            String lastName = Faker.instance().name().lastName();
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@migrate.com";
            LocalDate birthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            User u = new User();
            u.setIsActive(true);
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setEmail(email);
            u.setBirthDate(birthDate);
            u.setRole(em.find(UserRoles.class, (short) 1)); // Subscribed
            u.setLevel("débutant");
            u.setCreated(LocalDateTime.now());
            u.setAuth(null);

            savedUser = userService.newUser(u);

            // ACT & ASSERT - Try to migrate to SAME role
            assertThrows(ResponseStatusException.class, () -> {
                migrateUserDESTRUCTIVE.migrateUserRole(savedUser.getMatricule(), (short) 1); // Same role
            }, "Should throw BAD_REQUEST when migrating to same role");

            reporter.publishEntry("info", "Same role migration test passed - correctly rejected");
        }

        @Test
        @Order(2)
        void migrateNonExistentUserDB() {
            // ARRANGE
            String nonExistentMatricule = "Z1234"; // Assuming this matricule doesn't exist

            // ACT & ASSERT
            assertThrows(ResponseStatusException.class, () -> {
                migrateUserDESTRUCTIVE.migrateUserRole(nonExistentMatricule, (short) 2);
            }, "Should throw NOT_FOUND when user doesn't exist");

            reporter.publishEntry("info", "Non-existent user migration test passed - correctly rejected");
        }

        @Test
        @Order(3)
        void migrateUserWithActivePenaltyDB() {
            // ARRANGE
            String matricule = savedUser.getMatricule();

            // Add ACTIVE penalty (current time is within range)
            UserPenalties activePenalty = new UserPenalties();
            activePenalty.setUser(savedUser);
            activePenalty.setReason("unpaid_balance");
            activePenalty.setStartDate(LocalDateTime.now().minusDays(10)); // Started 10 days ago
            activePenalty.setEndDate(LocalDateTime.now().plusDays(10)); // Ends in 10 days
            activePenalty.setIsActive(true); //ACTIVE
            activePenalty.setDescription("Active penalty blocking migration");
            activePenalty.setMatchId(null); //NULL TO AVOID CONSTRAINT

            jpaUserPenaltiesRepo.save(activePenalty);

            // ACT & ASSERT - Try to migrate user with ACTIVE penalty
            assertThrows(ResponseStatusException.class, () -> {
                migrateUserDESTRUCTIVE.migrateUserRole(matricule, (short) 1);
            }, "Should throw CONFLICT when user has active penalties");

            // CLEANUP
            userService.deleteAllPenaltiesForUser(savedUser.getMatricule());


            reporter.publishEntry("info", "Active penalty migration test passed - correctly rejected");
        }

        @Test
        @Order(4)
        void migrateToNonExistentRoleDB() {
            // ARRANGE
            String matricule = savedUser.getMatricule();

            // ACT & ASSERT - Try to migrate to non-existent role
            assertThrows(ResponseStatusException.class, () -> {
                migrateUserDESTRUCTIVE.migrateUserRole(matricule, (short) 999); // Role doesn't exist
            }, "Should throw NOT_FOUND when role doesn't exist");

            reporter.publishEntry("info", "Non-existent role migration test passed - correctly rejected");
        }

        @Test
        @Order(5)
        void migrateEdgeCaseEmailDuplicateDB() {
            // ARRANGE
            User user1 = savedUser;
            // Create second user with different email
            String firstName2 = Faker.instance().name().firstName();
            String lastName2 = Faker.instance().name().lastName();
            String email2 = firstName2.toLowerCase() + "." + lastName2.toLowerCase() + "@migrate.com";
            LocalDate birthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            User user2 = new User();
            user2.setIsActive(true);
            user2.setFirstName(firstName2);
            user2.setLastName(lastName2);
            user2.setEmail(email2);
            user2.setBirthDate(birthDate);
            user2.setRole(em.find(UserRoles.class, (short) 0)); // Invite
            user2.setLevel("confirmé");
            user2.setCreated(LocalDateTime.now());
            user2.setAuth(null);

            User savedUser2 = userService.newUser(user2);
            String matricule2 = savedUser2.getMatricule();

            // ACT - Migration should succeed (emails are different)
            User migratedUser = migrateUserDESTRUCTIVE.migrateUserRole(matricule2, (short) 1);

            // ASSERT
            assertNotNull(migratedUser);
            assertTrue(migratedUser.getMatricule().startsWith("S"), "Migrated user should have S prefix");
            assertEquals(email2, migratedUser.getEmail(), "Email should be preserved");

            // CLEANUP
            userService.deleteUser(user1.getMatricule());
            userService.deleteUser(migratedUser.getMatricule());

            reporter.publishEntry("info", "Migration edge case with different emails handled correctly");
        }

        @Test
        @Order(7)
        void migrateNormalToAdminDB() {
            // ARRANGE
            String firstName = Faker.instance().name().firstName();
            String lastName = Faker.instance().name().lastName();
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@migrate.com";
            LocalDate birthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            // Create NORMAL user (not admin)
            User u = new User();
            u.setIsActive(true);
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setEmail(email);
            u.setBirthDate(birthDate);
            u.setRole(em.find(UserRoles.class, (short) 1)); // Subscribed (S) - Normal user
            u.setLevel("averti");
            u.setCreated(LocalDateTime.now());
            u.setAuth(null);

            User savedNormalUser = userService.newUser(u);

            // ACT & ASSERT - Try to migrate S(1) → M(7) (Normal user → Site Admin)
            assertThrows(ResponseStatusException.class, () -> {
                migrateUserDESTRUCTIVE.migrateUserRole(savedNormalUser.getMatricule(), (short) 7);
            }, "Should throw BAD_REQUEST when trying to migrate normal user to admin role");

            // CLEANUP
            userService.deleteUser(savedNormalUser.getMatricule());

            reporter.publishEntry("info", "Normal to admin role migration test passed - correctly rejected");
        }
    }
}
