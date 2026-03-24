package lu.ephec.backend_projetdv2026.services;

import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.models.SiteSessions;
import lu.ephec.backend_projetdv2026.services.interfaces.JPASitesRepo;
import lu.ephec.backend_projetdv2026.services.interfaces.JPASitesSessionsRepo;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Beans Injection to allow @BeforeAll non-static
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class SitesSessionsRepoLiveDbTests {

    @Autowired
    private SitesSessionsRepo sitesSessionsRepo;
    @Autowired
    private JPASitesSessionsRepo jpaSitesSessionsRepo;
    @Autowired
    private JPASitesRepo jpaSitesRepo; //To fetch Site

    private TestReporter reporter;

    //To save session ID
    private Integer savedSession;

    @BeforeEach
    void initReporter(TestReporter reporter) {
        this.reporter = reporter;
    }

    @Test
    @Order(1)
    void insertSessionDB() {
        // ARRANGE
        // PICK EXISTING SITE
        Integer site = jpaSitesRepo.findAll()
                .stream()
                .findAny()
                .map(Site::getSiteId)
                .orElseThrow(() -> new RuntimeException("No sites in DB"));

        // CREATE START / END TIMES
        LocalDateTime startTime = LocalDateTime.now().withNano(0).plusHours(1);
        LocalDateTime endTime = startTime.plusHours(2);

        // ACT
        SiteSessions ss = new SiteSessions();
        ss.setSiteId(site);
        ss.setStartTime(startTime);
        ss.setEndTime(endTime);

        // CALL
        SiteSessions saved = sitesSessionsRepo.newSite(ss);

        // ASSERT
        assertNotNull(saved);

        Optional<SiteSessions> fetchedById = sitesSessionsRepo.fetchById(saved.getSessionId());
        List<SiteSessions> fetchedByStart = sitesSessionsRepo.getSessionByStartTime(startTime);
        List<SiteSessions> fetchedByEnd = sitesSessionsRepo.getSessionByEndTime(endTime);

        assertAll("Verify saved session",
                () -> assertTrue(fetchedById.isPresent(),
                        () -> "Session not found by ID: " + saved.getSessionId()),
                () -> assertFalse(fetchedByStart.isEmpty(),
                        () -> "Session not found by Start Time: " + startTime),
                () -> assertEquals(startTime, fetchedById.get().getStartTime(),
                        () -> "Start time mismatch for session_id=" + saved.getSessionId()),
                () -> assertFalse(fetchedByEnd.isEmpty(),
                        () -> "Session not found by End Time: " + endTime),
                () -> assertEquals(endTime, fetchedById.get().getEndTime(),
                        () -> "End time mismatch for session_id=" + saved.getSessionId())
        );

        this.savedSession = saved.getSessionId(); //FOR UPDATE/DELETE TESTS
        reporter.publishEntry("info", "Inserted session sessionId=" + saved.getSessionId());
    }


    @Test
    @Order(2)
    void updateSessionDB() {
        // ARRANGE
        LocalDateTime newStart = LocalDateTime.now().withNano(0).plusDays(1).plusHours(10);
        LocalDateTime newEnd = newStart.plusHours(3);
        Integer sessionId = savedSession;

        // ACT
        // UPDATING SESSION: we create a new object with the same ID and updated fields, then save it (JPA will update since ID exists)
        SiteSessions updatedSession = new SiteSessions();
        updatedSession.setSessionId(sessionId);

        SiteSessions existing = jpaSitesSessionsRepo.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found for update: " + sessionId));

        updatedSession.setSiteId(existing.getSiteId());
        updatedSession.setStartTime(newStart);
        updatedSession.setEndTime(newEnd);

        // CALL
        SiteSessions saved = sitesSessionsRepo.newSite(updatedSession);

        // ASSERT
        assertNotNull(saved, "Updated session returned null for: " + sessionId);

        Optional<SiteSessions> fetched = sitesSessionsRepo.fetchById(sessionId);
        assertTrue(fetched.isPresent(), "Session not found after update: " + sessionId);
        SiteSessions updated = fetched.get();

        assertAll("Verify updated session",
                () -> assertEquals(newStart, updated.getStartTime(), "Start time not updated for: " + sessionId),
                () -> assertEquals(newEnd, updated.getEndTime(), "End time not updated for: " + sessionId)
        );

        reporter.publishEntry("info", "Updated session sessionId=" + sessionId);
    }

    @Test
    @Order(3)
    void deleteSessionDB() {
        // ARRANGE
        Integer sessionId = savedSession;

        // ACT
        sitesSessionsRepo.deleteSession(sessionId);

        // ASSERT
        assertTrue(sitesSessionsRepo.fetchById(sessionId).isEmpty(), "Session not deleted: " + sessionId);

        reporter.publishEntry("info", "Deleted session sessionId=" + sessionId);
    }
}
