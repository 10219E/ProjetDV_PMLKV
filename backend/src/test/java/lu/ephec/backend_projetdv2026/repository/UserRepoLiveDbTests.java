package lu.ephec.backend_projetdv2026.repository;

import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.repository.interfaces.JPAUserRepo;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Beans Injection to allow @BeforeAll non-static
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class UserRepoLiveDbTests {

    @Autowired //Easier to autowire the repo directly since it's a @Service that depends on the JPA repo, which is also a Spring Bean
    private UserRepo userRepo;
    @Autowired
    private JPAUserRepo jpaUserRepo;

    private TestReporter reporter; //REPORTER

    private String randomMatricule;

    @BeforeAll
    void initGenMatricule() {
        randomMatricule = jpaUserRepo.fetchRandomUserId().orElseThrow(() -> new RuntimeException("No users in DB"));
    }

    @BeforeEach
    void initReporter(TestReporter reporter) {
        this.reporter = reporter;
    }

    @Test
    @Order(1)
    void InsertUserDB() {

        //ARRANGE
        // generate a unique matricule such as MXXXX where XXXX is a random 4-digit number
        String matricule = "S" + (int)(Math.random() * 10000);
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
        u.setRoleId((short)1);
        u.setLevel("débutant");
        u.setCreated(LocalDateTime.now());
        u.setAuth(null);

        //CALL
        User saved = userRepo.newUser(u);

        //ASSERT
        assertNotNull(saved);

        //CALL
        Optional<User> fetchedById = userRepo.fetchById(matricule);
        Optional<User> fetchedByEmail = userRepo.fetchByMail(email);
        Optional<User> fetchedByFirstName = userRepo.fetchByName(firstName);
        Optional<User> fetchedByLastName = userRepo.fetchByName(lastName);

        assertAll("Verify saved user",
                () -> assertTrue(fetchedById.isPresent(),
                        () -> "User not found by ID: " + matricule),
                () -> assertTrue(fetchedByEmail.isPresent(),
                        () -> "User not found by Email: " + email),
                () -> assertEquals(firstName, fetchedById.get().getFirstName(),
                        () -> "First name mismatch for " + matricule),
                () -> assertEquals(firstName, fetchedByFirstName.get().getFirstName(),
                        ()  -> "User not found by First Name: " + firstName),
                () -> assertEquals(lastName, fetchedByLastName.get().getLastName(),
                        ()  -> "User not found by Last Name: " + lastName)
        );

        reporter.publishEntry("info", "Inserted user matricule=" + saved.getMatricule());
    }


    //PROVIDER FOR TEST 2
    Stream<String> matriculeProvider() {
        return Stream.of(randomMatricule);
    }

    @ParameterizedTest
    @MethodSource("matriculeProvider")
    @Order(2)
    void UpdateUserDB(String matricule) {
        //ARRANGE
        String newFirstName = Faker.instance().name().firstName();
        String newEmail = newFirstName.toLowerCase() + "." + Faker.instance().name().lastName().toLowerCase() + "@example.com";
        Short newRoleId = 0;
        String newMatricule = "L"+(int)(Math.random() * 10000);
        String newLevel = "confirmé"; //Invalid level to test partial update (should not be updated)
        LocalDate newBirthDate = Faker.instance().date().birthday(18, 65).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        //ACT
        User updatedUser = new User();
        updatedUser.setFirstName(newFirstName);
        updatedUser.setEmail(newEmail);
        updatedUser.setRoleId(newRoleId);
        updatedUser.setMatricule(newMatricule);
        updatedUser.setLevel(newLevel); //should fail as invalid by the DB
        updatedUser.setBirthDate(newBirthDate);

        //CALL
        Optional<User> updatedOpt = userRepo.updUser(matricule, updatedUser);

        //ASSERT
        assertTrue(updatedOpt.isPresent(), "User not found for update: " + matricule);
        User updated = updatedOpt.get();

        assertAll("Verify updated user",
                () -> assertEquals(newFirstName, updated.getFirstName(),
                        "First name not updated for: " + matricule),
                () -> assertEquals(newEmail, updated.getEmail(),
                        "Email not updated for: " + matricule),
                () -> assertEquals(newRoleId, updated.getRoleId(),
                        "Role not updated for: " + matricule),
                () -> assertNotEquals(newMatricule, updated.getMatricule(),
                        "Matricule should not have changed for: " + matricule),
                () -> assertEquals(newBirthDate, updated.getBirthDate(),
                        "Birthdate not updated for: " + matricule),
                () -> assertEquals(newLevel, updated.getLevel(),
                        "Level wrongly updated for: " + matricule)
        );

        reporter.publishEntry("info", "Updated user matricule=" + matricule);
    }


    @Test
    @Order(3)
    void DeleteUserDB() {
        //ARRANGE
        String matricule = randomMatricule;

        //ACT
        userRepo.delUser(matricule);

        //ASSERT
        assertTrue(userRepo.fetchById(matricule).isEmpty(), "User not deleted: " + matricule);

        reporter.publishEntry("info", "Deleted user matricule=" + matricule);
    }
}

