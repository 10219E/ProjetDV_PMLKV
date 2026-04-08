package lu.ephec.backend_projetdv2026.services;

import lu.ephec.backend_projetdv2026.InitBaseH2Test;
import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.repo.JPAFieldRepo;
import com.github.javafaker.Faker; //USING FAKER TO GEN INFO
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class FieldServiceH2DbTests extends InitBaseH2Test {

    @Autowired
    private FieldService fieldService;
    @Autowired
    private SiteService siteService;
    @Autowired
    private JPAFieldRepo jpaFieldRepo;

    private TestReporter reporter;

    private Integer savedFieldId;
    private Site savedSite;

    @BeforeEach
    void initReporter(TestReporter reporter) {
        this.reporter = reporter;
    }

    @BeforeAll
    void setupTestSite() {
        // CREATE TEST SITE FOR ALL FIELD TESTS
        Site testSite = new Site();
        testSite.setName(Faker.instance().artist().name() + " " + (int) (Math.random() * 10000));
        testSite.setAddress(Faker.instance().address().streetAddress());
        testSite.setOpeningTime(LocalTime.of(8, 0));
        testSite.setClosingTime(LocalTime.of(17, 0));

        savedSite = siteService.newSite(testSite);
        //this.testSiteId = savedSite.getSiteId();
    }

    @AfterAll
    void cleanupTestSite() {
        // CLEANUP TEST SITE (which will cascade delete all fields)
        if (savedSite.getSiteId() != null) {
            siteService.deleteSite(savedSite.getSiteId());
        }
    }

    /// SITE OPS
    @Nested
    @DisplayName("CRUD - SiteService Tests")
    class FieldsCrudOperations {
        @Test
        @Order(1)
        void insertFieldDB() {
            // ARRANGE
            Boolean isIndoor = Faker.instance().bool().bool();
            Boolean isActive = true;
            LocalDate maintenanceFrom = LocalDate.now().plusDays(1);
            LocalDate maintenanceTo = maintenanceFrom.plusDays(7);

            // ACT
            Field f = new Field();
            f.setSite(savedSite);
            f.setIsIndoor(isIndoor);
            f.setMaintenanceFromDate(maintenanceFrom);
            f.setMaintenanceToDate(maintenanceTo);

            // CALL
            Field saved = fieldService.newField(f);

            // ASSERT
            assertNotNull(saved);

            Optional<Field> fetchedById = fieldService.fetchById(saved.getFieldId());
            List<Field> fetchedBySite = fieldService.fetchBySite(savedSite.getSiteId());

            assertAll("Verify saved field",
                    () -> assertTrue(fetchedById.isPresent(),
                            () -> "Field not found by ID: " + saved.getFieldId()),
                    () -> assertTrue(fetchedBySite.stream()
                                    .anyMatch(x -> x.getFieldId().equals(saved.getFieldId())),
                            () -> "Field not found in list by Site: " + savedSite.getSiteId()),
                    () -> assertEquals(isIndoor, fetchedById.get().getIsIndoor(),
                            () -> "isIndoor mismatch for field_id=" + saved.getFieldId()),
                    () -> assertEquals(isActive, fetchedById.get().getIsActive(),
                            () -> "isActive mismatch for field_id=" + saved.getFieldId()),
                    () -> assertEquals(maintenanceFrom, fetchedById.get().getMaintenanceFromDate(),
                            () -> "maintenanceFrom mismatch for field_id=" + saved.getFieldId()),
                    () -> assertEquals(maintenanceTo, fetchedById.get().getMaintenanceToDate(),
                            () -> "maintenanceTo mismatch for field_id=" + saved.getFieldId())
            );

            savedFieldId = saved.getFieldId(); // TO USE IN UPDATE And DELETE

            reporter.publishEntry("info", "Inserted field fieldId=" + saved.getFieldId());
        }

        @Test
        @Order(2)
        void updateFieldDB() {
            // ARRANGE
            Integer fieldId = savedFieldId;
            Boolean newIsIndoor = !Boolean.TRUE.equals(jpaFieldRepo.findById(fieldId)
                    .map(Field::getIsIndoor).orElse(Boolean.FALSE)); //INVERT isIndoor
            Boolean newIsActive = false;
            LocalDate newMaintenanceFrom = LocalDate.now().plusDays(5);
            LocalDate newMaintenanceTo = newMaintenanceFrom.plusDays(3);

            Field uField = new Field();
            uField.setIsIndoor(newIsIndoor);
            uField.setIsActive(newIsActive);
            uField.setMaintenanceFromDate(newMaintenanceFrom);
            uField.setMaintenanceToDate(newMaintenanceTo);

            // CALL
            Optional<Field> updatedOpt = fieldService.updateField(fieldId, uField);

            // ASSERT
            assertTrue(updatedOpt.isPresent(), "Field not found for update: " + fieldId);
            Field updated = updatedOpt.get();

            assertAll("Verify updated field",
                    () -> assertEquals(newIsIndoor, updated.getIsIndoor(), "isIndoor not updated for: " + fieldId),
                    () -> assertEquals(newIsActive, updated.getIsActive(), "isActive not updated for: " + fieldId),
                    () -> assertEquals(newMaintenanceFrom, updated.getMaintenanceFromDate(), "maintenanceFrom not updated for: " + fieldId),
                    () -> assertEquals(newMaintenanceTo, updated.getMaintenanceToDate(), "maintenanceTo not updated for: " + fieldId)
            );

            reporter.publishEntry("info", "Updated field fieldId=" + fieldId);
        }

        @Test
        @Order(3)
        void deleteFieldDB() {
            // ARRANGE
            Integer fieldId = savedFieldId;

            // ACT
            fieldService.deleteField(fieldId);

            // ASSERT
            assertFalse(fieldService.fieldExists(fieldId), "Field still exists after deletion: " + fieldId);

            reporter.publishEntry("info", "Deleted field fieldId=" + fieldId);
        }

        @Test
        @Order(4)
        void fetchIndoorOutdoorDB() {
            //INDOOR
            Field f1 = new Field();
            f1.setSite(savedSite);
            f1.setIsIndoor(Boolean.TRUE);
            f1.setIsActive(Boolean.TRUE);

            //OUTDOOR
            Field f2 = new Field();
            f2.setSite(savedSite);
            f2.setIsIndoor(Boolean.FALSE);
            f2.setIsActive(Boolean.TRUE);

            // ACT
            Field savedIndoor = fieldService.newField(f1);
            Field savedOutdoor = fieldService.newField(f2);

            try {
                // CALL
                List<Field> indoorList = fieldService.fetchIndoorBySite(savedSite.getSiteId());
                List<Field> outdoorList = fieldService.fetchOutdoorBySite(savedSite.getSiteId());

                // ASSERT: the lists must contain at least the saved entries
                assertTrue(indoorList.stream().anyMatch(f -> f.getFieldId().equals(savedIndoor.getFieldId())),
                        "Saved indoor field not found in fetchIndoor()");
                assertTrue(outdoorList.stream().anyMatch(f -> f.getFieldId().equals(savedOutdoor.getFieldId())),
                        "Saved outdoor field not found in fetchOutdoor()");

            } finally {
                // CLEANUP
                fieldService.deleteField(savedIndoor.getFieldId());
                fieldService.deleteField(savedOutdoor.getFieldId());
            }

            reporter.publishEntry("info", "Fetch indoor/outdoor OK (indoorId=" + savedIndoor.getFieldId()
                    + ", outdoorId=" + savedOutdoor.getFieldId() + ")");

        }

    }

    /// EXCEPTIONS
    @Nested
    @DisplayName("EXCEPTIONS - SiteService Tests")
    class FieldExceptionTests {
        @Test
        @Order(1)
        void newFieldWithInvalidDatesDB() {
            // ARRANGE
            Field f = new Field();
            f.setSite(savedSite);
            f.setIsIndoor(true);
            f.setMaintenanceFromDate(LocalDate.now().plusDays(10));
            f.setMaintenanceToDate(LocalDate.now().plusDays(5));  // To date before from date

            // ACT & ASSERT
            assertThrows(ResponseStatusException.class, () -> {
                fieldService.newField(f);
            }, "Should throw BAD_REQUEST when fromDate > toDate");

            reporter.publishEntry("info", "newField invalid dates test passed - correctly rejected");
        }

        @Test
        @Order(2)
        void fetchByIdWithNonExistentFieldDB() {
            // ACT & ASSERT
            assertThrows(ResponseStatusException.class, () -> {
                fieldService.fetchById(99999);
            }, "Should throw NOT_FOUND when field doesn't exist");

            reporter.publishEntry("info", "fetchById non-existent field test passed - correctly rejected");
        }

    }

}


