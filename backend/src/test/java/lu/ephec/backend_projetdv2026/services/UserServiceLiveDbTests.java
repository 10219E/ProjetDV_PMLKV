package lu.ephec.backend_projetdv2026.services;

import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.models.UserPenalties;
import lu.ephec.backend_projetdv2026.repo.JPAUserRepo;
import com.github.javafaker.Faker;  //USING FAKER TO GEN INFO
import org.junit.jupiter.api.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lu.ephec.backend_projetdv2026.models.UserRoles;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Beans Injection to allow @BeforeAll non-static
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class UserServiceLiveDbTests {

    @Autowired //Easier to AutoWire (@Service SpringBean)
    private UserService userService;
    @Autowired
    private JPAUserRepo jpaUserRepo;

    private TestReporter reporter; //REPORTER

    private String savedMatricule; //Reusing Matricule for CLEANUP and DELETE Test

    private String ExcepSavedMatricule;

    private Integer savedPenaltyTr;

    private String ExcepSavedEmail;



    //private String randomMatricule; Removing Random as not clean for Live Tests

    @PersistenceContext
    private EntityManager em; //TOOL TO Check user roles

    @BeforeEach
    void initReporter(TestReporter reporter) {
        this.reporter = reporter;
    }


    /////////CRUD TESTS////////

    @Nested
    @DisplayName("CRUD - UserService Tests")
    class CrudTests {

        /// USER TEST
        @Test
        @Order(1)
        void insertUserDB() {

            //ARRANGE
            //String matricule = "S" + (int) (Math.random() * 10000); //Auto Generated
            String firstName = Faker.instance().name().firstName();
            String lastName = Faker.instance().name().lastName();
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com";
            LocalDate birthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            //ACT
            User u = new User();
            //u.setMatricule(matricule);
            u.setIsActive(true);
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setEmail(email);
            u.setBirthDate(birthDate);
            u.setRole(em.find(UserRoles.class, (short) 0));
            u.setLevel("débutant");
            u.setCreated(LocalDateTime.now());
            u.setAuth(null);

            //CALL
            User saved = userService.newUser(u);

            //ARRANGE2
            String matricule = saved.getMatricule(); //FETCH GENERATED MATRICULE FOR FURTHER TESTS

            //ASSERT
            assertNotNull(saved);

            Optional<User> fetchedById = userService.fetchById(matricule);
            Optional<User> fetchedByEmail = userService.fetchByMail(email);
            List<User> fetchedByFirstName = userService.fetchByName(firstName);
            List<User> fetchedByLastName = userService.fetchByName(lastName);

            assertAll("Verify saved user",
                    () -> assertTrue(fetchedById.isPresent(),
                            () -> "User not found by ID: " + matricule),
                    () -> assertTrue(fetchedByEmail.isPresent(),
                            () -> "User not found by Email: " + email),
                    () -> assertEquals(firstName, fetchedById.get().getFirstName(),
                            () -> "First name mismatch for " + matricule),
                    () -> assertFalse(fetchedByFirstName.isEmpty(),
                            () -> "User not found by First Name: " + firstName),
                    () -> assertTrue(
                            fetchedByFirstName.stream().anyMatch(usr -> usr.getMatricule().equals(matricule)),
                            () -> "Inserted user not found in results for firstName: " + firstName),
                    () -> assertFalse(fetchedByLastName.isEmpty(),
                            () -> "User not found by Last Name: " + lastName),
                    () -> assertTrue(
                            fetchedByLastName.stream().anyMatch(usr -> usr.getMatricule().equals(matricule)),
                            () -> "Inserted user not found in results for lastName: " + lastName)
            );

            savedMatricule = matricule; //TO BE USED IN DELETE

            reporter.publishEntry("info", "Inserted user matricule=" + saved.getMatricule());
        }


        //PROVIDER FOR TEST 2 and 4
        /*Stream<String> matriculeProvider() {
            return Stream.of(randomMatricule);
        }*/

        //@ParameterizedTest
        //@MethodSource("matriculeProvider") //APPLY TOP 1 (MAYBE CORRECT LATER AS LIVE DB)
        @Test
        @Order(2)
        void updateUserDB() {
            //ARRANGE
            String matricule = savedMatricule;
            String newFirstName = Faker.instance().name().firstName();
            String newEmail = newFirstName.toLowerCase() + "." + Faker.instance().name().lastName().toLowerCase() + "@example.com";
            //Short newRoleId = 0;
            String newMatricule = "L" + (int) (Math.random() * 10000); //SHOULD NOT UPDATE
            String newLevel = "confirmé";
            LocalDate newBirthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            //ACT
            User updatedUser = new User();
            updatedUser.setFirstName(newFirstName);
            updatedUser.setEmail(newEmail);
            //updatedUser.setRole(em.find(UserRoles.class, newRoleId));
            updatedUser.setMatricule(newMatricule);
            updatedUser.setLevel(newLevel);
            updatedUser.setBirthDate(newBirthDate);

            //CALL
            Optional<User> updatedOpt = userService.updateUser(matricule, updatedUser);

            //ASSERT
            assertTrue(updatedOpt.isPresent(), "User not found for update: " + matricule);
            User updated = updatedOpt.get();

            assertAll("Verify updated user",
                    () -> assertEquals(newFirstName, updated.getFirstName(),
                            "First name not updated for: " + matricule),
                    () -> assertEquals(newEmail, updated.getEmail(),
                            "Email not updated for: " + matricule),
                    //() -> assertEquals(newRoleId, updated.getRole().getId(),
                    //        "Role not updated for: " + matricule),
                    () -> assertNotEquals(newMatricule, updated.getMatricule(),
                            "Matricule should not have changed for: " + matricule), //PRIMARY KEY CAN NOT BE UPDATED
                    () -> assertEquals(newBirthDate, updated.getBirthDate(),
                            "Birthdate not updated for: " + matricule),
                    () -> assertEquals(newLevel, updated.getLevel(),
                            "Level wrongly updated for: " + matricule)
            );

            reporter.publishEntry("info", "Updated user matricule=" + matricule);
        }


        @Test
        @Order(3)
        void deleteUserDB() {
            //ARRANGE
            String matricule = savedMatricule;

            //ACT
            userService.deleteUser(matricule);

            //ASSERT
            //assertTrue(userService.fetchById(matricule).isEmpty(), "User not deleted: " + matricule); CAN'T USE THAT ANYMORE DUE TO VALIDATION
            assertFalse(userService.userExists(matricule), "User not deleted: " + matricule);

            reporter.publishEntry("info", "Deleted user matricule=" + matricule);
        }

        @Test
        @Order(4)
        void sameNameUserSearchTest() {
            //ARRANGE
            //String matricule1 = "S" + (int) (Math.random() * 10000); //AUTO HANDLED
            //String matricule2 = "L" + (int) (Math.random() * 10000); //AUTO HANDLED

            String firstName1 = Faker.instance().name().firstName();
            String lastName2 = firstName1;

            String firstName2 = Faker.instance().name().firstName();
            String lastName1 = firstName2;

            String email1 = firstName1.toLowerCase() + "." + lastName1.toLowerCase() + "@example.com";
            String email2 = firstName2.toLowerCase() + "." + lastName2.toLowerCase() + "@example.com";

            LocalDate birthDate1 = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            LocalDate birthDate2 = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            //ACT
            User u1 = new User();
            //u1.setMatricule(matricule1);
            u1.setIsActive(true);
            u1.setFirstName(firstName1);
            u1.setLastName(lastName1);
            u1.setEmail(email1);
            u1.setBirthDate(birthDate1);
            u1.setRole(em.find(UserRoles.class, (short) 1));
            u1.setLevel("débutant");
            u1.setCreated(LocalDateTime.now());
            u1.setAuth(null);

            User u2 = new User();
            //u2.setMatricule(matricule2);
            u2.setIsActive(false);
            u2.setFirstName(firstName2);
            u2.setLastName(lastName2);
            u2.setEmail(email2);
            u2.setBirthDate(birthDate2);
            u2.setRole(em.find(UserRoles.class, (short) 1));
            u2.setLevel("confirmé");
            u2.setCreated(LocalDateTime.now());
            u2.setAuth(null);

            //CALL
            User saved1 = userService.newUser(u1);
            User saved2 = userService.newUser(u2);

            //ARRANGE2
            String matricule1 = saved1.getMatricule();
            String matricule2 = saved2.getMatricule();

            // ASSERT
            //Match firstName1 (which is also lastName of u2)
            List<User> resultsForName1 = userService.fetchByName(firstName1);
            assertFalse(resultsForName1.isEmpty(), () -> "No results for name: " + firstName1);
            assertTrue(resultsForName1.stream().anyMatch(u -> u.getMatricule().equals(matricule1)),
                    () -> "Inserted user1 not found when searching for: " + firstName1);
            assertTrue(resultsForName1.stream().anyMatch(u -> u.getMatricule().equals(matricule2)),
                    () -> "Inserted user2 not found when searching for: " + firstName1);

            //Match firstName2 (which is also lastName of u1)
            List<User> resultsForName2 = userService.fetchByName(firstName2);
            assertFalse(resultsForName2.isEmpty(), () -> "No results for name: " + firstName2);
            assertTrue(resultsForName2.stream().anyMatch(u -> u.getMatricule().equals(matricule1)),
                    () -> "Inserted user1 not found when searching for: " + firstName2);
            assertTrue(resultsForName2.stream().anyMatch(u -> u.getMatricule().equals(matricule2)),
                    () -> "Inserted user2 not found when searching for: " + firstName2);

            //CLEANUP
            userService.deleteUser(matricule1);
            //userService.deleteUser(matricule2); //keeping User2 for further tests
            savedMatricule = matricule2;

            reporter.publishEntry("info", "sameNameUserSearchTest inserted and verified matricules=" + saved1.getMatricule() + "," + saved2.getMatricule());


        }

        /// PENALTIES TEST

        @Test
        @Order(5)
        void insertPenaltyDB() {
            // ARRANGE
            String userId = savedMatricule; //Use previous test (4) UserId
            String reason = "unpaid_balance";
            Integer matchid = 5;
            LocalDateTime startDate = LocalDateTime.now();
            LocalDateTime endDate = LocalDateTime.now().plusDays(30);
            String description = "Unpaid tournament fee";

            // ACT
            UserPenalties penalty = new UserPenalties();
            penalty.setUser(jpaUserRepo.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId)));
            penalty.setReason(reason);
            penalty.setStartDate(startDate);
            penalty.setEndDate(endDate);
            penalty.setIsActive(true);
            penalty.setMatchId(matchid);
            penalty.setDescription(description);

            UserPenalties saved = userService.newPenalty(penalty);

            // ASSERT
            assertNotNull(saved);
            assertNotNull(saved.getTr());
            assertEquals(reason, saved.getReason());
            assertTrue(saved.getIsActive());

            savedPenaltyTr = saved.getTr(); //TO BE USED IN UPDATE

            reporter.publishEntry("info", "Inserted penalty id=" + saved.getTr());
        }

        @Test
        @Order(6)
        void checkActivePenaltyDB() {
            // ARRANGE
            String userId = savedMatricule;

            // ACT
            boolean hasActivePenalty = userService.hasActivePenalty(userId);

            // ASSERT
            assertTrue(hasActivePenalty,
                    () -> "User should have an active penalty: " + userId);

            reporter.publishEntry("info", "Penalty is active for " + userId);
        }

        @Test
        @Order(7)
        void updatePenaltyDB() { //+DEACTIVATE
            // ARRANGE
            Integer penaltyId = savedPenaltyTr;
            LocalDateTime newEndDate = LocalDateTime.now().plusDays(60);
            String newDescription = "Updated: Extended penalty period";

            // ACT
            UserPenalties updatedPenalty = new UserPenalties();
            updatedPenalty.setEndDate(newEndDate);
            updatedPenalty.setDescription(newDescription);
            updatedPenalty.setIsActive(false); // Deactivate

            Optional<UserPenalties> updatedOpt = userService.updatePenalty(penaltyId, updatedPenalty);

            // ASSERT
            assertTrue(updatedOpt.isPresent(), "Penalty not found: " + penaltyId);
            UserPenalties updated = updatedOpt.get();

            assertAll("Verify updated penalty",
                    () -> assertEquals(newEndDate, updated.getEndDate(),
                            "EndDate not updated for penalty: " + penaltyId),
                    () -> assertEquals(newDescription, updated.getDescription(),
                            "Description not updated for penalty: " + penaltyId),
                    () -> assertFalse(updated.getIsActive(),
                            "Penalty should be inactive for: " + penaltyId)
            );

            savedPenaltyTr = updated.getTr();

            reporter.publishEntry("info", "Updated penalty id=" + penaltyId);

        }

        @Test
        @Order(8)
        void checkInactivePenaltyDB() {
            // ARRANGE
            String userId = savedMatricule;
            Integer penaltyId = savedPenaltyTr;

            // ACT
            boolean hasActivePenalty = userService.hasActivePenalty(userId);

            // ASSERT
            assertFalse(hasActivePenalty,
                    () -> "User should NOT have an active penalty after deactivation: " + userId);

            //CLEANUP
            userService.deletePenalty(penaltyId);
            userService.deleteUser(userId);

            reporter.publishEntry("info", "User " + userId + " has NO active penalty after deactivation");

        }
    }


    /////////EXCEPTION TESTS////////

    @Nested
    @DisplayName("EXCEPTION - UserService Tests")
    class NegativeTests {

        @Test
        @Order(1)
        void insertUserWithDuplicateEmailDB() {
            // ARRANGE
            String firstName1 = Faker.instance().name().firstName();
            String lastName1 = Faker.instance().name().lastName();
            String firstName2 = Faker.instance().name().firstName();
            String lastName2 = Faker.instance().name().lastName();

            String email = firstName1.toLowerCase() + "." + lastName1.toLowerCase() + "@example.com";

            LocalDate birthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
            //String duplicateEmail = ExcepSavedEmail;

            // Create first user
            User u1 = new User();
            u1.setIsActive(true);
            u1.setFirstName(firstName1);
            u1.setLastName(lastName1);
            u1.setEmail(email);
            u1.setBirthDate(birthDate);
            u1.setRole(em.find(UserRoles.class, (short)1));
            u1.setLevel("débutant");
            u1.setCreated(LocalDateTime.now());
            u1.setAuth(null);

            User saved = userService.newUser(u1); //Save user

            // Try to create second user with SAME email
            User u = new User();
            //u.setMatricule(matricule);
            u.setIsActive(true);
            u.setFirstName(firstName2);
            u.setLastName(lastName2);
            u.setEmail(email); //SHOULD FAIL AS DUPE
            u.setBirthDate(birthDate);
            u.setRole(em.find(UserRoles.class, (short)1));
            u.setLevel("confirmé");
            u.setCreated(LocalDateTime.now());
            u.setAuth(null);

            //SUB-ARRANGE
            //String matricule = u.getMatricule(); Would not work as user is not created yet

            // ACT & ASSERT
            assertThrows(ResponseStatusException.class, () -> {
                userService.newUser(u);
            }, "Should throw CONFLICT when email already exists");

            ExcepSavedMatricule = saved.getMatricule(); //To be used in further delete
            ExcepSavedEmail = saved.getEmail();

            reporter.publishEntry("info", "Duplicate email test passed - correctly rejected");
        }

        @Test
        @Order(2)
        void fetchByMailNonExistentDB() {
            // ARRANGE
            String nonExistentEmail = "nonexistent@example.com";

            // ACT & ASSERT
            assertThrows(ResponseStatusException.class, () -> {
                userService.fetchByMail(nonExistentEmail);
            }, "Should throw NOT_FOUND when email doesn't exist");

            reporter.publishEntry("info", "Fetch by non-existent email test passed - correctly rejected");
        }

        @Test
        @Order(3)
        void insertUserWithInvalidLevelDB() {
            // ARRANGE
            //String matricule = "S" + (int)(Math.random() * 10000);
            String firstName = Faker.instance().name().firstName();
            String lastName = Faker.instance().name().lastName();
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com";
            LocalDate birthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            // Try to create user with INVALID level
            User u = new User();
            //u.setMatricule(matricule);
            u.setIsActive(true);
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setEmail(email);
            u.setBirthDate(birthDate);
            u.setRole(em.find(UserRoles.class, (short) 1));
            u.setLevel("invalid_level"); // SHOULD FAIL - not in (débutant, averti, confirmé)
            u.setCreated(LocalDateTime.now());
            u.setAuth(null);

            //Sub-ARRANGE
            //String matricule = u.getMatricule();

            // ACT & ASSERT
            assertThrows(ResponseStatusException.class, () -> {
                userService.newUser(u);
            }, "Should throw BAD_REQUEST when level is invalid");

            reporter.publishEntry("info", "Invalid level test passed - correctly rejected");
        }

        @Test
        @Order(4)
        void updateUserWithDuplicateEmailDB() {
            // ARRANGE
            //String matricule = "S" + (int)(Math.random() * 10000);
            String firstName = Faker.instance().name().firstName();
            String lastName = Faker.instance().name().lastName();

            LocalDate birthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

            String dummy_email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com";
            String email = ExcepSavedEmail;

            //Using SAME EMAIL for update
            User u = new User();
            //u.setMatricule(matricule);
            u.setIsActive(true);
            u.setFirstName(firstName);
            u.setLastName(lastName);
            u.setEmail(dummy_email); //BEFORE UPD
            u.setBirthDate(birthDate);
            u.setRole(em.find(UserRoles.class, (short)1));
            u.setLevel("confirmé");
            u.setCreated(LocalDateTime.now());
            u.setAuth(null);

            User saved = userService.newUser(u);

            //TRY EXISTING EMAIL ON NEW USER
            User updateData = new User();
            updateData.setEmail(email); //EMAIL ALREADY IN USE

            String matricule = saved.getMatricule();

            // ACT & ASSERT
            assertThrows(ResponseStatusException.class, () -> {
                userService.updateUser(matricule, updateData);
            }, "Should throw CONFLICT when trying to update to existing email");

            // CLEANUP
            userService.deleteUser(ExcepSavedMatricule);

            ExcepSavedMatricule = matricule; //USE FOR PENALTIES

            reporter.publishEntry("info", "Update user with duplicate email test passed - correctly rejected");
        }


        @Test
        @Order(5)
        void deleteNonExistentUserDB() {
            // ARRANGE
            String nonExistentUserId = "L000001";

            // ACT & ASSERT
            assertThrows(ResponseStatusException.class, () -> {
                userService.deleteUser(nonExistentUserId);
            }, "Should throw NOT_FOUND when user doesn't exist");

            reporter.publishEntry("info", "Delete non-existent user test passed - correctly rejected");
        }

        /// PENALTIES TEST
        @Test
        @Order(6)
        void insertPenaltyWithInvalidDatesDB() {
            // ARRANGE
            LocalDateTime endDate = LocalDateTime.now();
            LocalDateTime startDate = endDate.plusDays(30);//START DATE INVALID

            //REUSING USER
            String userId = ExcepSavedMatricule;


            // Try to create penalty with INVALID dates
            UserPenalties penalty = new UserPenalties();
            penalty.setUser(jpaUserRepo.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found")));
            penalty.setReason("unpaid_balance");
            penalty.setStartDate(startDate); //SHOULD FAIL HERE
            penalty.setEndDate(endDate);
            penalty.setIsActive(true);
            penalty.setDescription("Test");

            // ACT & ASSERT
            assertThrows(ResponseStatusException.class, () -> {
                userService.newPenalty(penalty);
            }, "Should throw BAD_REQUEST when start date > end date");

            reporter.publishEntry("info", "Invalid dates test passed - correctly rejected");
        }


        @Test
        @Order(7)
        void insertPenaltyWithNullReasonDB() {
            // ARRANGE
            String userId = ExcepSavedMatricule;

            // Try to create penalty WITHOUT reason
            UserPenalties penalty = new UserPenalties();
            penalty.setUser(jpaUserRepo.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found")));
            penalty.setReason(null); //SHOULD FAIL HERE (no reason provided or invalid reason)
            penalty.setStartDate(LocalDateTime.now());
            penalty.setEndDate(LocalDateTime.now().plusDays(30));
            penalty.setIsActive(true);
            penalty.setDescription("Test");

            // ACT & ASSERT
            assertThrows(ResponseStatusException.class, () -> {
                userService.newPenalty(penalty);
            }, "Should throw BAD_REQUEST when reason is null");

            // CLEANUP
            userService.deleteUser(userId);

            reporter.publishEntry("info", "Null reason test passed - correctly rejected");
        }


        @Test
        @Order(8)
        void deletePenaltyForNonExistentUserDB() {
            // ARRANGE
            String nonExistentUserId = ExcepSavedMatricule; //DELETED IN PREVIOUS TEST

            // ACT & ASSERT
            assertThrows(ResponseStatusException.class, () -> {
                userService.deleteAllPenaltiesForUser(nonExistentUserId);
            }, "Should throw NOT_FOUND when user doesn't exist");

            reporter.publishEntry("info", "Delete penalties for non-existent user test passed - correctly rejected");
        }

        @Test
        @Order(9)
        void deletePenaltyWithNonExistentIdDB() {
            // ARRANGE
            Integer nonExistentPenaltyId = 99999; //DOESN'T EXIST

            // ACT & ASSERT
            assertThrows(ResponseStatusException.class, () -> {
                userService.deletePenalty(nonExistentPenaltyId);
            }, "Should throw NOT_FOUND when penalty ID doesn't exist");

            reporter.publishEntry("info", "Delete non-existent penalty test passed - correctly rejected");
        }

    }

}



