package lu.ephec.backend_projetdv2026.services;

import com.github.javafaker.Faker;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lu.ephec.backend_projetdv2026.InitBaseH2Test;
import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.models.UserRoles;
import lu.ephec.backend_projetdv2026.models.UsersSites;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class UserSiteSubServiceH2DbTests extends InitBaseH2Test {

    @Autowired
    private UserSiteSubService userSiteSubService;
    @Autowired
    private UserService userService;
    @Autowired
    private SiteService siteService;

    @PersistenceContext
    private EntityManager em;

    private TestReporter reporter;

    @BeforeEach
    void initReporter(TestReporter reporter) {
        this.reporter = reporter;
    }

    // ===== HELPER METHODS =====
    private User createTestUser() {
        User user = new User();
        String firstName = Faker.instance().name().firstName();
        String lastName = Faker.instance().name().lastName();
        String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com";
        LocalDate birthDate = Faker.instance().date().birthday(18, 65).toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate();

        user.setIsActive(true);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setBirthDate(birthDate);
        user.setRole(em.find(UserRoles.class, (short) 1));
        user.setLevel("débutant");
        user.setCreated(LocalDateTime.now());
        user.setAuth(null);

        return userService.newUser(user);
    }

    @Test
    @Order(1)
    void newUserSiteDB() {
        // ARRANGE
        List<Site> allSites = siteService.fetchAll();
        assertFalse(allSites.isEmpty(), "No sites available in database");
        Site site = allSites.get(0);
        Integer siteId = site.getSiteId();

        User user = createTestUser();
        String userId = user.getMatricule();

        // ACT
        UsersSites link = userSiteSubService.newUserSite(userId, siteId, false, true);

        // ASSERT
        assertNotNull(link);
        assertEquals(userId, link.getUser().getMatricule());
        assertEquals(siteId, link.getSite().getSiteId());
        assertFalse(link.getIsPrimary());
        assertTrue(link.getIsVip());

        List<UsersSites> byUser = userSiteSubService.fetchByUser(userId);
        assertTrue(byUser.stream().anyMatch(l ->
                        l.getSite().getSiteId().equals(siteId)
                                && Boolean.TRUE.equals(l.getIsVip())
                                && Boolean.FALSE.equals(l.getIsPrimary())),
                "Created user-site link not found by fetchByUser");

        // CLEANUP
        userService.deleteUser(userId);

        reporter.publishEntry("info", "newUserSiteDB passed (user=" + userId + ", site=" + siteId + ")");
    }

    @Test
    @DisplayName("EXCEPTION - SA CAN'T BE ASSIGNED TO SITE")
    @Order(2)
    void newUserSiteSuperAdminForbiddenDB() {
        // ARRANGE
        List<Site> allSites = siteService.fetchAll();
        assertFalse(allSites.isEmpty(), "No sites available in database");
        Site site = allSites.get(0);
        Integer siteId = site.getSiteId();

        User superAdmin = new User();
        String firstName = "SA" + Faker.instance().number().digits(4);
        String lastName = Faker.instance().name().lastName();
        String email = "sa." + Faker.instance().number().digits(6) + "@example.com";

        superAdmin.setIsActive(true);
        superAdmin.setFirstName(firstName);
        superAdmin.setLastName(lastName);
        superAdmin.setEmail(email);
        superAdmin.setBirthDate(LocalDate.of(1990, 1, 1));
        superAdmin.setRole(em.find(UserRoles.class, (short) 9));
        superAdmin.setLevel(null);
        superAdmin.setCreated(LocalDateTime.now());
        superAdmin.setAuth(null);

        User savedSa = userService.newUser(superAdmin);
        String superAdminId = savedSa.getMatricule();

        // ACT & ASSERT
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                userSiteSubService.newUserSite(superAdminId, siteId, true, false)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());

        // CLEANUP
        userService.deleteUser(superAdminId);

        reporter.publishEntry("info", "newUserSiteSuperAdminForbiddenDB passed (SA=" + superAdminId + ", site=" + siteId + ")");
    }
}
