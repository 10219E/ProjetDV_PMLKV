package lu.ephec.backend_projetdv2026.repository;

import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.repository.interfaces.JPASitesRepo;
import com.github.javafaker.Faker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalTime;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Beans Injection to allow @BeforeAll non-static
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class SitesRepoLiveDbTests {

    @Autowired
    private SitesRepo sitesRepo;
    @Autowired
    private JPASitesRepo jpaSitesRepo;

    private TestReporter reporter;

    private Integer randomSiteId;

    private Integer savedSiteId;

    @BeforeAll
    void initGenSiteId() {
        //GET TOP1
        randomSiteId = jpaSitesRepo.findAll()
                .stream()
                .findFirst()
                .map(Site::getSiteId)
                .orElseThrow(() -> new RuntimeException("No sites in DB"));
    }

    @BeforeEach
    void initReporter(TestReporter reporter) {
        this.reporter = reporter;
    }

    @Test
    @Order(1)
    void InsertSiteDB() {
        // ARRANGE
        String name = Faker.instance().artist().name() + " " + (int)(Math.random() * 10000);
        String address = Faker.instance().address().streetAddress();
        LocalTime openingTime = LocalTime.of(8, 0);
        LocalTime closingTime = LocalTime.of(17, 0);

        // ACT
        Site s = new Site();
        s.setName(name);
        s.setAddress(address);
        s.setOpeningTime(openingTime);
        s.setClosingTime(closingTime);
        s.setIsActive(true);

        // CALL
        Site saved = sitesRepo.newSite(s);

        // ASSERT
        assertNotNull(saved);

        Optional<Site> fetchedById = sitesRepo.fetchById(saved.getSiteId());
        Optional<Site> fetchedByName = sitesRepo.fetchByName(name);
        Optional<Site> fetchedByAddress = sitesRepo.fetchByAddress(address);

        assertAll("Verify saved site",
                () -> assertTrue(fetchedById.isPresent(),
                        () -> "Site not found by ID: " + saved.getSiteId()),
                () -> assertTrue(fetchedByName.isPresent(),
                        () -> "Site not found by Name: " + name),
                () -> assertEquals(name, fetchedById.get().getName(),
                        () -> "Name mismatch for site_id=" + saved.getSiteId()),
                () -> assertEquals(name, fetchedByName.get().getName(),
                        ()  -> "Site not found by Name: " + name),
                () -> assertEquals(address, fetchedByAddress.get().getAddress(),
                        ()  -> "Site not found by Address: " + address)
        );

        this.savedSiteId = saved.getSiteId(); //TO USE IN DELETE

        reporter.publishEntry("info", "Inserted site siteId=" + saved.getSiteId());
    }

    // PROVIDER FOR TEST 2
    Stream<Integer> siteIdProvider() {
        return Stream.of(randomSiteId);
    }

    @ParameterizedTest
    @MethodSource("siteIdProvider") //APPLY TOP 1
    @Order(2)
    void UpdateSiteDB(Integer siteId) {
        // ARRANGE
        String newName = Faker.instance().artist().name() + " " + (int)(Math.random() * 10000);
        String newAddress = Faker.instance().address().streetAddress();
        //Integer newSiteId = (Integer)(Math.random() * 10000); DB HANDLED
        LocalTime newOpeningTime = LocalTime.of(9, 30);
        LocalTime newClosingTime = LocalTime.of(18, 0);
        Boolean newIsActive = false;

        // ACT
        Site updatedSite = new Site();
        updatedSite.setName(newName);
        updatedSite.setAddress(newAddress);
        updatedSite.setOpeningTime(newOpeningTime);
        updatedSite.setClosingTime(newClosingTime);
        updatedSite.setIsActive(newIsActive);

        // CALL
        Optional<Site> updatedOpt = sitesRepo.updSite(siteId, updatedSite);

        // ASSERT
        assertTrue(updatedOpt.isPresent(), "Site not found for update: " + siteId);
        Site updated = updatedOpt.get();

        assertAll("Verify updated site",
                () -> assertEquals(newName, updated.getName(),
                        "Name not updated for: " + siteId),
                () -> assertEquals(newAddress, updated.getAddress(),
                        "Address not updated for: " + siteId),
                () -> assertEquals(newOpeningTime, updated.getOpeningTime(),
                        "Opening time not updated for: " + siteId),
                () -> assertEquals(newClosingTime, updated.getClosingTime(),
                        "Closing time not updated for: " + siteId),
                () -> assertEquals(newIsActive, updated.getIsActive(),
                        "isActive not updated for: " + siteId)
        );

        reporter.publishEntry("info", "Updated site siteId=" + siteId);
    }

    @Test
    @Order(3)
    void DeleteSiteDB() {
        // ARRANGE
        Integer siteId = savedSiteId;

        // ACT
        sitesRepo.deleteSite(siteId);

        // ASSERT
        assertTrue(sitesRepo.fetchById(siteId).isEmpty(), "Site not deleted: " + siteId);

        reporter.publishEntry("info", "Deleted site siteId=" + siteId);
    }
}
