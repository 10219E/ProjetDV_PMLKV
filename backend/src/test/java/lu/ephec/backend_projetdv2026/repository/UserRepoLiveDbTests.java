package lu.ephec.backend_projetdv2026.repository;

import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.models.UserPenalties;
import lu.ephec.backend_projetdv2026.repository.interfaces.JPAUserRepo;
import com.github.javafaker.Faker;  //USING FAKER TO GEN INFO
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lu.ephec.backend_projetdv2026.models.UserRoles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Beans Injection to allow @BeforeAll non-static
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class UserRepoLiveDbTests {

    @Autowired //Easier to AutoWire (@Service SpringBean)
    private UserRepo userRepo;
    @Autowired
    private JPAUserRepo jpaUserRepo;

    private TestReporter reporter; //REPORTER

    private String savedMatricule; //Reusing Matricule for CLEANUP and DELETE Test

    private Integer savedPenaltyTr;

    //private String randomMatricule; Removing Random as not clean for Live Tests

    @PersistenceContext
    private EntityManager em; //TOOL TO Check user roles

    /*@BeforeAll
    void initGenMatricule() { //GET TOP 1
        randomMatricule = jpaUserRepo.findAll()
                .stream()
                .findFirst()
                .map(User::getMatricule)
                .orElseThrow(() -> new RuntimeException("No sites in DB"));

    }*/

    @BeforeEach
    void initReporter(TestReporter reporter) {
        this.reporter = reporter;
    }

    @Test
    @Order(1)
    void insertUserDB() {

        //ARRANGE
        String matricule = "S" + (int)(Math.random() * 10000); //Generate unique matricule
        String firstName = Faker.instance().name().firstName();
        String lastName = Faker.instance().name().lastName();
        String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com";
        LocalDate birthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        //ACT
        User u = new User();
        u.setMatricule(matricule);
        u.setIsActive(true);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setBirthDate(birthDate);
        u.setRole(em.find(UserRoles.class, (short)1));
        u.setLevel("débutant");
        u.setCreated(LocalDateTime.now());
        u.setAuth(null);

        //CALL
        User saved = userRepo.newUser(u);

        //ASSERT
        assertNotNull(saved);

        Optional<User> fetchedById = userRepo.fetchById(matricule);
        Optional<User> fetchedByEmail = userRepo.fetchByMail(email);
        List<User> fetchedByFirstName = userRepo.fetchByName(firstName);
        List<User> fetchedByLastName  = userRepo.fetchByName(lastName);

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

        savedMatricule = saved.getMatricule(); //TO BE USED IN DELETE

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
        Short newRoleId = 0;
        String newMatricule = "L"+(int)(Math.random() * 10000);
        String newLevel = "confirmé";
        LocalDate newBirthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        //ACT
        User updatedUser = new User();
        updatedUser.setFirstName(newFirstName);
        updatedUser.setEmail(newEmail);
        updatedUser.setRole(em.find(UserRoles.class, newRoleId));
        updatedUser.setMatricule(newMatricule);
        updatedUser.setLevel(newLevel);
        updatedUser.setBirthDate(newBirthDate);

        //CALL
        Optional<User> updatedOpt = userRepo.updateUser(matricule, updatedUser);

        //ASSERT
        assertTrue(updatedOpt.isPresent(), "User not found for update: " + matricule);
        User updated = updatedOpt.get();

        assertAll("Verify updated user",
                () -> assertEquals(newFirstName, updated.getFirstName(),
                        "First name not updated for: " + matricule),
                () -> assertEquals(newEmail, updated.getEmail(),
                        "Email not updated for: " + matricule),
                () -> assertEquals(newRoleId, updated.getRole().getId(),
                        "Role not updated for: " + matricule),
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
        userRepo.deleteUser(matricule);

        //ASSERT
        assertTrue(userRepo.fetchById(matricule).isEmpty(), "User not deleted: " + matricule);

        reporter.publishEntry("info", "Deleted user matricule=" + matricule);
    }

    @Test
    @Order(4)
    void sameNameUserSearchTest() {
        //ARRANGE
        String matricule1 = "S" + (int)(Math.random() * 10000);
        String matricule2 = "L" + (int)(Math.random() * 10000);

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
        u1.setMatricule(matricule1);
        u1.setIsActive(true);
        u1.setFirstName(firstName1);
        u1.setLastName(lastName1);
        u1.setEmail(email1);
        u1.setBirthDate(birthDate1);
        u1.setRole(em.find(UserRoles.class, (short)1));
        u1.setLevel("débutant");
        u1.setCreated(LocalDateTime.now());
        u1.setAuth(null);

        User u2 = new User();
        u2.setMatricule(matricule2);
        u2.setIsActive(false);
        u2.setFirstName(firstName2);
        u2.setLastName(lastName2);
        u2.setEmail(email2);
        u2.setBirthDate(birthDate2);
        u2.setRole(em.find(UserRoles.class, (short)1));
        u2.setLevel("confirmé");
        u2.setCreated(LocalDateTime.now());
        u2.setAuth(null);

        //CALL
        User saved1 = userRepo.newUser(u1);
        User saved2 = userRepo.newUser(u2);

        // ASSERT
        //Match firstName1 (which is also lastName of u2)
        List<User> resultsForName1 = userRepo.fetchByName(firstName1);
        assertFalse(resultsForName1.isEmpty(), () -> "No results for name: " + firstName1);
        assertTrue(resultsForName1.stream().anyMatch(u -> u.getMatricule().equals(matricule1)),
                () -> "Inserted user1 not found when searching for: " + firstName1);
        assertTrue(resultsForName1.stream().anyMatch(u -> u.getMatricule().equals(matricule2)),
                () -> "Inserted user2 not found when searching for: " + firstName1);

        //Match firstName2 (which is also lastName of u1)
        List<User> resultsForName2 = userRepo.fetchByName(firstName2);
        assertFalse(resultsForName2.isEmpty(), () -> "No results for name: " + firstName2);
        assertTrue(resultsForName2.stream().anyMatch(u -> u.getMatricule().equals(matricule1)),
                () -> "Inserted user1 not found when searching for: " + firstName2);
        assertTrue(resultsForName2.stream().anyMatch(u -> u.getMatricule().equals(matricule2)),
                () -> "Inserted user2 not found when searching for: " + firstName2);

        //CLEANUP
        userRepo.deleteUser(matricule1);
        //userRepo.deleteUser(matricule2); //keeping User2 for further tests
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
        Integer matchid = 4;
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.now().plusDays(30);
        String description = "Unpaid tournament fee";

        // ACT
        UserPenalties penalty = new UserPenalties();
        penalty.setUser(jpaUserRepo.findById(userId).get());
        penalty.setReason(reason);
        penalty.setStartDate(startDate);
        penalty.setEndDate(endDate);
        penalty.setIsActive(true);
        penalty.setMatchId(matchid);
        penalty.setDescription(description);

        UserPenalties saved = userRepo.newPenalty(penalty);

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
        boolean hasActivePenalty = userRepo.hasActivePenalty(userId);

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

        Optional<UserPenalties> updatedOpt = userRepo.updatePenalty(penaltyId, updatedPenalty);

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
        boolean hasActivePenalty = userRepo.hasActivePenalty(userId);

        // ASSERT
        assertFalse(hasActivePenalty,
                () -> "User should NOT have an active penalty after deactivation: " + userId);

        //CLEANUP
        userRepo.deletePenalty(penaltyId);
        userRepo.deleteUser(userId);

        reporter.publishEntry("info", "User " + userId + " has NO active penalty after deactivation");

    }


}



