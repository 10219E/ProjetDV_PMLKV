package lu.ephec.backend_projetdv2026.services;

import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.models.SiteClosureDays;
import lu.ephec.backend_projetdv2026.repo.JPASiteRepo;
import com.github.javafaker.Faker;  //USING FAKER TO GEN INFO
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Beans Injection to allow @BeforeAll non-static
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class SiteServiceLiveDbTests {

    @Autowired
    private SiteService siteService;
    @Autowired
    private JPASiteRepo jpaSiteRepo;

    private TestReporter reporter;

    private Site savedSite;

    private List<SiteClosureDays> savedClosures;


    @BeforeEach
    void initReporter(TestReporter reporter) {
        this.reporter = reporter;
    }

    @Nested
    @DisplayName("CRUD - SiteService Tests")
    class CrudTests {

        /// SITE OPERATIONS ///
        @Test
        @Order(1)
        void insertSiteDB() {
            // ARRANGE
            String name = Faker.instance().artist().name() + " " + (int) (Math.random() * 10000);
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
            Site saved = siteService.newSite(s);

            // ASSERT
            assertNotNull(saved);

            Optional<Site> fetchedById = siteService.fetchById(saved.getSiteId());
            Optional<Site> fetchedByName = siteService.fetchByName(name);
            Optional<Site> fetchedByAddress = siteService.fetchByAddress(address);

            assertAll("Verify saved site",
                    () -> assertTrue(fetchedById.isPresent(),
                            () -> "Site not found by ID: " + saved.getSiteId()),
                    () -> assertTrue(fetchedByName.isPresent(),
                            () -> "Site not found by Name: " + name),
                    () -> assertEquals(name, fetchedById.get().getName(),
                            () -> "Name mismatch for site_id=" + saved.getSiteId()),
                    () -> assertEquals(name, fetchedByName.get().getName(),
                            () -> "Site not found by Name: " + name),
                    () -> assertEquals(address, fetchedByAddress.get().getAddress(),
                            () -> "Site not found by Address: " + address)
            );

            savedSite = saved; //TO USE IN DELETE

            reporter.publishEntry("info", "Inserted site siteId=" + saved.getSiteId());
        }

        @Test
        @Order(2)
        void updateSiteDB() {
            // ARRANGE
            String newName = Faker.instance().artist().name() + " " + (int) (Math.random() * 10000);
            String newAddress = Faker.instance().address().streetAddress();
            //Integer newSiteId = (Integer)(Math.random() * 10000); //DB HANDLED
            LocalTime newOpeningTime = LocalTime.of(9, 0);
            LocalTime newClosingTime = LocalTime.of(18, 0);
            Boolean newIsActive = false;

            // ACT
            Site updatedSite = new Site();
            updatedSite.setName(newName);
            updatedSite.setAddress(newAddress);
            updatedSite.setOpeningTime(newOpeningTime);
            updatedSite.setClosingTime(newClosingTime);
            updatedSite.setIsActive(newIsActive);

            Integer siteId = savedSite.getSiteId();
            // CALL
            Optional<Site> updatedOpt = siteService.updateSite(siteId, updatedSite);

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
        void deleteSiteDB() {
            // ARRANGE
            Integer siteId = savedSite.getSiteId();

            // ACT
            siteService.deleteSite(siteId);

            // ASSERT
            assertFalse(siteService.siteExists(siteId), "Site not deleted: " + siteId);

            reporter.publishEntry("info", "Deleted site siteId=" + siteId);
        }

        /// CLOSURE DAYS TESTS ///
        @Test
        @Order(4)
        void insertClosureDaysDB() {
            // ARRANGE
            //Site
            String name = Faker.instance().artist().name() + " " + (int) (Math.random() * 10000);
            String address = Faker.instance().address().streetAddress();
            LocalTime openingTime = LocalTime.of(8, 0);
            LocalTime closingTime = LocalTime.of(17, 0);

            //Closure
            LocalDate closureDate1 = LocalDate.now().plusDays(3);
            LocalDate closureDate2 = LocalDate.now().plusDays(15);
            String reason = Faker.instance().company().catchPhrase() + " Holiday";

            // ACT
            Site s = new Site();
            s.setName(name);
            s.setAddress(address);
            s.setOpeningTime(openingTime);
            s.setClosingTime(closingTime);
            s.setIsActive(true);

            // CALL SITE
            Site saved = siteService.newSite(s);

            // ACT - Create Closure Days with ONE call / ONE REASON1
            List<SiteClosureDays> closures = siteService.newClosuresOneSite(
                    saved.getSiteId(),
                    List.of(closureDate1, closureDate2),
                    reason
            );

            // ASSERT
            assertNotNull(closures);
            assertEquals(2, closures.size(), "Should have created 2 closures");

            SiteClosureDays closure1 = closures.get(0);
            SiteClosureDays closure2 = closures.get(1);

            assertAll("Verify saved closures",
                    () -> assertEquals(saved.getSiteId(), closure1.getSiteId(),
                            "Site ID mismatch for closure date 1"),
                    () -> assertEquals(closureDate1, closure1.getClosureDate(),
                            "Closure date mismatch for closure date 1"),
                    () -> assertEquals(reason, closure1.getReason(),
                            "Reason mismatch for closure date 1"),
                    () -> assertEquals(saved.getSiteId(), closure2.getSiteId(),
                            "Site ID mismatch for closure date 1"),
                    () -> assertEquals(closureDate2, closure2.getClosureDate(),
                            "Closure date mismatch for closure date 2"),
                    () -> assertEquals(reason, closure2.getReason(),
                            "Reason mismatch for closure date 2")
            );

            savedClosures = closures;
            savedSite = saved;

            reporter.publishEntry("info", "Inserted " + closures.size() + " closures for site=" + saved.getSiteId());
        }

        @Test
        @Order(5)
        void fetchClosureByDateRangeDB() {
            // ARRANGE
            //Closure
            LocalDate closureDate1 = LocalDate.now().plusDays(1);
            LocalDate closureDate2 = LocalDate.now().plusDays(30);
            String reason1 = Faker.instance().company().catchPhrase() + " Cool Off";
            String reason2 = Faker.instance().company().catchPhrase() + " Inventory";

            LocalDate startDate = savedClosures.get(0).getClosureDate().minusDays(1);
            LocalDate endDate = savedClosures.get(1).getClosureDate().plusDays(2);

            // CALL SITE
            Site saved = savedSite;

            //EXTRA CLOSURES FOR CHECKS
            // ACT - Create Closure Days with different reasons
            siteService.newClosuresOneSite(saved.getSiteId(), List.of(closureDate1), reason1);
            siteService.newClosuresOneSite(saved.getSiteId(), List.of(closureDate2), reason2);

            // ACT
            List<SiteClosureDays> closureRange = siteService.fetchClosuresByDateRange(startDate, endDate);

            // ASSERT
            assertNotNull(closureRange);
            assertEquals(2, closureRange.size(), "Should have found 2 closures");

            assertAll("Verify closures in date range",
                    () -> assertTrue(closureRange.stream().allMatch(c -> !c.getClosureDate().isBefore(startDate)),
                            "Some closures are before start date"),
                    () -> assertTrue(closureRange.stream().allMatch(c -> !c.getClosureDate().isAfter(endDate)),
                            "Some closures are after end date")
            );

            reporter.publishEntry("info", "Fetched " + closureRange.size() + " closures in date range");
        }

        @Test
        @Order(6)
        void fetchClosuresBySiteDB() {
            // ACT
            List<SiteClosureDays> closures = siteService.fetchClosureForSite(savedSite.getSiteId());

            // ASSERT
            assertNotNull(closures);
            assertEquals(4, closures.size(), "Should have found 4 closures");

            assertAll("Verify closures for site: " + savedSite.getSiteId(),
                    () -> assertTrue(closures.stream().allMatch(c -> c.getSiteId().equals(savedSite.getSiteId())),
                            "Some closures do not match site ID")
            );

            reporter.publishEntry("info", "Fetched " + closures.size() + " closures for site=" + savedSite.getSiteId());
        }

        @Test
        @Order(8)
        void deleteClosureDayForSiteDB() {
            // ARRANGE
            Integer siteId = savedSite.getSiteId();

            // ACT
            siteService.deleteAllClosuresForSite(siteId);
            List<SiteClosureDays> closures = siteService.fetchClosureForSite(siteId);

            // ASSERT
            assertTrue(closures.isEmpty(), "Closures not deleted");
            assertTrue(siteService.fetchClosureForSite(siteId).isEmpty(), "Closures not deleted for site: " + siteId);

            //CLEANUP
            siteService.deleteSite(siteId);

            reporter.publishEntry("info", "Successfully deleted closures for site=" + siteId);
        }

        @Test
        @Order(9)
        void insertMultipleSitesClosuresDB() {
            // ARRANGE
            Site s1 = new Site();
            s1.setName(Faker.instance().artist().name() + " " + (int)(Math.random() * 10000));
            s1.setAddress(Faker.instance().address().streetAddress());
            s1.setOpeningTime(LocalTime.of(16, 0));
            s1.setClosingTime(LocalTime.of(22, 0));
            s1.setIsActive(true);

            Site s2 = new Site();
            s2.setName(Faker.instance().artist().name() + " " + (int)(Math.random() * 10000));
            s2.setAddress(Faker.instance().address().streetAddress());
            s2.setOpeningTime(LocalTime.of(9, 0));
            s2.setClosingTime(LocalTime.of(18, 0));
            s2.setIsActive(true);

            Site saved1 = siteService.newSite(s1);
            Site saved2 = siteService.newSite(s2);
            LocalDate closureDate = LocalDate.now().plusDays(25);
            String reason = Faker.instance().company().catchPhrase();

            // ACT
            List<SiteClosureDays> result = siteService.newClosureMultiSite(
                    List.of(saved1.getSiteId(), saved2.getSiteId()),
                    List.of(closureDate),
                    reason
            );

            // ASSERT
            assertEquals(2, result.size());
            assertTrue(result.stream().allMatch(c -> c.getClosureDate().equals(closureDate)));

            //CLEANUP
            siteService.deleteSite(saved1.getSiteId());
            siteService.deleteSite(saved2.getSiteId());

            reporter.publishEntry("info", "Created closures for 2 sites");
        }

    }


    @Nested
    @DisplayName("EXCEPTION - SiteService Tests")
    class ExceptionTests {

        /// SITE OPS ///
        @Test
        @Order(1)
        void insertSiteWithEmptyNameDB() {
            // ARRANGE
            Site s = new Site();
            s.setName("");
            s.setAddress(Faker.instance().address().streetAddress());
            s.setOpeningTime(LocalTime.of(8, 0));
            s.setClosingTime(LocalTime.of(17, 0));

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                siteService.newSite(s);
            });

            reporter.publishEntry("info", "Correctly rejected empty site name");
        }

        /// CLOSURE DAYS TESTS ///

        @Test
        @Order(2)
        void insertClosureWithNonExistentSiteDB() {
            // ARRANGE
            Integer invalidSiteId = 999999;
            LocalDate closureDate = LocalDate.now().plusDays(10);

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                siteService.newClosuresOneSite(invalidSiteId, List.of(closureDate), "Holiday");
            });

            reporter.publishEntry("info", "Correctly rejected closure for non-existent site");
        }

        @Test
        @Order(3)
        void insertClosureWithPastDateDB() {
            // ARRANGE
            LocalDate pastDate = LocalDate.now().minusDays(1);
            // ARRANGE
            Site s = new Site();
            s.setName(Faker.instance().artist().name() + " " + (int) (Math.random() * 10000));
            s.setAddress(Faker.instance().address().streetAddress());
            s.setOpeningTime(LocalTime.of(8, 0));
            s.setClosingTime(LocalTime.of(17, 0));

            //CALL
            savedSite = siteService.newSite(s);

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                siteService.newClosuresOneSite(savedSite.getSiteId(), List.of(pastDate), "Holiday");
            });

            //CLEANUP
            siteService.deleteSite(savedSite.getSiteId());

            reporter.publishEntry("info", "Correctly rejected closure with past date");
        }

        @Test
        @Order(4)
        void fetchClosureByInvalidSiteDB() {
            // ARRANGE
            Integer invalidSiteId = savedSite.getSiteId();

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                siteService.fetchClosureForSite(invalidSiteId);
            });

            reporter.publishEntry("info", "Correctly rejected fetch for non-existent site");
        }

        @Test
        @Order(5)
        void deleteClosureByInvalidSiteDB() {
            // ARRANGE
            Integer inavalidSiteId = savedSite.getSiteId();

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
                siteService.deleteClosureDayForSite(inavalidSiteId, LocalDate.now().plusDays(5));
            });

            reporter.publishEntry("info", "Correctly rejected delete for non-existent site and closure");
        }

        /// HOURS TESTS ///

        @Test
        @Order(6)
        void insertSiteWithInvalidHoursDB() {
            // ARRANGE: 16:00–23:00 should be rejected (post > 30 min after last session)
            Site s = new Site();
            s.setName(Faker.instance().artist().name() + " " + (int)(Math.random() * 10000));
            s.setAddress(Faker.instance().address().streetAddress());
            s.setOpeningTime(LocalTime.of(16, 0));
            s.setClosingTime(LocalTime.of(23, 0));
            s.setIsActive(true);

            // ACT & ASSERT
            assertThrows(org.springframework.web.server.ResponseStatusException.class,
                    () -> siteService.newSite(s),
                    "Expected invalid hours to be rejected (16:00-23:00)");

            //CLEANUP IN CASE
            if (s.getSiteId() != null && siteService.siteExists(s.getSiteId())) {
                siteService.deleteSite(s.getSiteId());
            }

            reporter.publishEntry("info", "Correctly rejected site with invalid hours (16:00-23:00) for business reasons");
        }

    }
}
