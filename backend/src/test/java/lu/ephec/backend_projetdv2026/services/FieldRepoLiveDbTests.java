package lu.ephec.backend_projetdv2026.services;

import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.services.interfaces.JPAFieldRepo;
import lu.ephec.backend_projetdv2026.services.interfaces.JPASitesRepo;
import com.github.javafaker.Faker; //USING FAKER TO GEN INFO
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class FieldRepoLiveDbTests {

    @Autowired
    private FieldRepo fieldRepo;
    @Autowired
    private JPAFieldRepo jpaFieldRepo;
    @Autowired
    private JPASitesRepo jpaSitesRepo;

    private TestReporter reporter;

    private Integer savedFieldId;
    private Integer randomSiteId;

    @BeforeAll
    void initGenSiteId() {
        // GET TOP 1 Site (for insertion relation)
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
    void insertFieldDB() {
        // ARRANGE
        Boolean isIndoor = Faker.instance().bool().bool();
        Boolean isActive = true;
        LocalDate maintenanceFrom = LocalDate.now().plusDays(1);
        LocalDate maintenanceTo = maintenanceFrom.plusDays(7);

        //CHECK if Site exists
        Site site = jpaSitesRepo.findById(randomSiteId)
                .orElseThrow(() -> new RuntimeException("Site not found for id=" + randomSiteId));

        // ACT
        Field f = new Field();
        f.setSiteId(site.getSiteId());
        f.setIsIndoor(isIndoor);
        f.setIsActive(isActive);
        f.setMaintenanceFromDate(maintenanceFrom);
        f.setMaintenanceToDate(maintenanceTo);

        // CALL
        Field saved = fieldRepo.newField(f);

        // ASSERT
        assertNotNull(saved);

        Optional<Field> fetchedById = fieldRepo.fetchById(saved.getFieldId());
        List<Field> fetchedBySite = fieldRepo.fetchBySite(site.getSiteId());

        assertAll("Verify saved field",
                () -> assertTrue(fetchedById.isPresent(),
                        () -> "Field not found by ID: " + saved.getFieldId()),
                () -> assertTrue(fetchedBySite.stream()
                                .anyMatch(x -> x.getFieldId().equals(saved.getFieldId())),
                        () -> "Field not found in list by Site: " + site.getSiteId()),
                () -> assertEquals(isIndoor, fetchedById.get().getIsIndoor(),
                        () -> "isIndoor mismatch for field_id=" + saved.getFieldId()),
                () -> assertEquals(isActive, fetchedById.get().getIsActive(),
                        () -> "isActive mismatch for field_id=" + saved.getFieldId()),
                () -> assertEquals(maintenanceFrom, fetchedById.get().getMaintenanceFromDate(),
                        () -> "maintenanceFrom mismatch for field_id=" + saved.getFieldId()),
                () -> assertEquals(maintenanceTo, fetchedById.get().getMaintenanceToDate(),
                        () -> "maintenanceTo mismatch for field_id=" + saved.getFieldId())
        );

        this.savedFieldId = saved.getFieldId(); // TO USE IN UPDATE And DELETE

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
        //Integer newSite = site_id;
        LocalDate newMaintenanceFrom = LocalDate.now().plusDays(5);
        LocalDate newMaintenanceTo = newMaintenanceFrom.plusDays(3);

        Field updatedField = new Field();
        updatedField.setIsIndoor(newIsIndoor);
        updatedField.setIsActive(newIsActive);
        //updatedField.setSiteId(newSite); Not needed - not implemented
        updatedField.setMaintenanceFromDate(newMaintenanceFrom);
        updatedField.setMaintenanceToDate(newMaintenanceTo);

        // CALL
        Optional<Field> updatedOpt = fieldRepo.updField(fieldId, updatedField);

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
        fieldRepo.deleteField(fieldId);

        // ASSERT
        assertTrue(fieldRepo.fetchById(fieldId).isEmpty(), "Field not deleted: " + fieldId);

        reporter.publishEntry("info", "Deleted field fieldId=" + fieldId);
    }


    @Test
    @Order(4)
    void fetchIndoorOutdoorDB() {
        Site site = jpaSitesRepo.findById(randomSiteId)
                .orElseThrow(() -> new RuntimeException("Site not found for id=" + randomSiteId));

        //INDOOR
        Field indoor = new Field();
        indoor.setSiteId(site.getSiteId());
        indoor.setIsIndoor(Boolean.TRUE);
        indoor.setIsActive(Boolean.TRUE);

        //OUTDOOR
        Field outdoor = new Field();
        outdoor.setSiteId(site.getSiteId());
        outdoor.setIsIndoor(Boolean.FALSE);
        outdoor.setIsActive(Boolean.TRUE);

        // ACT
        Field savedIndoor = fieldRepo.newField(indoor);
        Field savedOutdoor = fieldRepo.newField(outdoor);

        try {
            // CALL
            List<Field> indoorList = fieldRepo.fetchIndoor();
            List<Field> outdoorList = fieldRepo.fetchOutdoor();

            // ASSERT: the lists must contain at least the saved entries
            assertTrue(indoorList.stream().anyMatch(f -> f.getFieldId().equals(savedIndoor.getFieldId())),
                    "Saved indoor field not found in fetchIndoor()");
            assertTrue(outdoorList.stream().anyMatch(f -> f.getFieldId().equals(savedOutdoor.getFieldId())),
                    "Saved outdoor field not found in fetchOutdoor()");

            reporter.publishEntry("info", "Fetch indoor/outdoor OK (indoorId=" + savedIndoor.getFieldId()
                    + ", outdoorId=" + savedOutdoor.getFieldId() + ")");

        } finally {
            // CLEANUP: delete created fields to avoid polluting DB
            fieldRepo.deleteField(savedIndoor.getFieldId());
            fieldRepo.deleteField(savedOutdoor.getFieldId());
        }
    }

}
