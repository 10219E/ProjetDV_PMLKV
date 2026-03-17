package lu.ephec.backend_projetdv2026.repository;

import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.repository.interfaces.JPAUserRepo;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class UserRepoLiveDbTests {

    @Autowired //Easier to autowire the repo directly since it's a @Service that depends on the JPA repo, which is also a Spring Bean
    private UserRepo userRepo;

    @Test
    void InsertUserDB() {
        // generate a unique matricule such as MXXXX where XXXX is a random 4-digit number
        String matricule = "S" + (int)(Math.random() * 10000);
        String firstName = Faker.instance().name().firstName();
        String lastName = Faker.instance().name().lastName();
        String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com";
        Date birthDate = Faker.instance().date().birthday(18, 65);

        User u = new User();
        u.setMatricule(matricule);
        u.setIsActive(true);
        u.setFirstName(firstName);
        u.setLastName(lastName);
        u.setEmail(email);
        u.setBirthDate(birthDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        u.setRoleId((short)1);
        u.setLevel("débutant");
        u.setCreated(LocalDateTime.now());
        u.setAuth(null);

        // call the method under test
        User saved = userRepo.newUser(u);
        assertNotNull(saved);

        // read back from repo
        Optional<User> fetched = userRepo.fetchById(matricule);
        assertTrue(fetched.isPresent(), "User should be found in DB after save");
        assertEquals(firstName, fetched.get().getFirstName());
    }
}

