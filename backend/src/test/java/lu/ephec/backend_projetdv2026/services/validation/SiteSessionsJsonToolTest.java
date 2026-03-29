package lu.ephec.backend_projetdv2026.services.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class SiteSessionsJsonToolTest {

    @Autowired
    private SiteSessionsJsonHandler handler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TestReporter reporter;

    @BeforeEach
    void initReporter(TestReporter reporter) {
        this.reporter = reporter;
    }

    @Test
    @Order(1)
    @DisplayName("Sessions for 16h00-22h00 hours.")
    void multiplePrePost30SessionsJson() throws Exception {
        // ARRANGE
        LocalTime opening = LocalTime.of(16, 0); //5H 3 SESSIONS
        LocalTime closing = LocalTime.of(22, 0);

        // ACT
        String json = handler.generateSessionsJson(opening, closing);
        JsonNode sessions = objectMapper.readTree(json);
        int sessionCount = sessions.get("sessions").size();

        // ASSERT
        assertTrue(sessionCount > 0, "Should generate at least one session");
        assertTrue(sessionCount == 3, "16h00-22h00 should fit 3 sessions");
        assertEquals("sessions", sessions.fieldNames().next(), "Root should contain 'sessions' field");

        JsonNode firstSession = sessions.get("sessions").get(0);
        assertEquals(1, firstSession.get("match_set_id").asInt());
        assertEquals(90, firstSession.get("duration_minutes").asInt());

        // CHECK FIRST SESSION START TIME
        LocalTime firstSessionStart = LocalTime.parse(firstSession.get("start_time").asText());
        LocalTime expectedStart = LocalTime.of(16, 30);  // 16:00 + 30 min pre-session FOR OPTIMIZATION
        assertEquals(expectedStart, firstSessionStart,
                "First session should start at 17:00 (opening 16:30 + 30 min pre-session)");

        //LAST SESSION END TIME
        JsonNode lastSession = sessions.get("sessions").get(sessionCount-1); //last session
        LocalTime lastSessionEnd = LocalTime.parse(lastSession.get("end_time").asText());
        LocalTime expectedEnd = LocalTime.of(21, 30);
        assertEquals(expectedEnd,lastSessionEnd,
                "Last session should end at 21:30 (opening 16:00, 3 sessions of 1h30 with 2 breaks of 15 min, 30 min pre/post-session)");

        //REPORTER
        reporter.publishEntry("info", sessionCount + " sessions created for 16h00-22h00 hours");
    }

    @Test
    @Order(2)
    @DisplayName("Sessions for 16h30-22h00 hours")
    void multiplePrePost15SessionsJson() throws Exception {
        // ARRANGE
        LocalTime opening = LocalTime.of(16, 30); //4H30 3 SESSIONS
        LocalTime closing = LocalTime.of(22, 0);

        // ACT
        String json = handler.generateSessionsJson(opening, closing);
        JsonNode sessions = objectMapper.readTree(json);
        int sessionCount = sessions.get("sessions").size();

        // ASSERT
        assertTrue(sessionCount > 0, "Should generate at least one session");
        assertTrue(sessionCount == 3, "16h30-22h00 should fit 3 sessions");

        JsonNode firstSession = sessions.get("sessions").get(0);

        // Verify JSON Values
        assertEquals("sessions", sessions.fieldNames().next(), "Root should contain 'sessions' field");
        assertEquals(1, firstSession.get("match_set_id").asInt());
        assertEquals(90, firstSession.get("duration_minutes").asInt());

        // CHECK FIRST SESSION START TIME
        LocalTime firstSessionStart = LocalTime.parse(firstSession.get("start_time").asText());
        LocalTime expectedStart = LocalTime.of(16, 45);  // 16:30 + 15 min pre-session FOR OPTIMIZATION
        assertEquals(expectedStart, firstSessionStart,
                "First session should start at 16:45 (opening 16:30 + 15 min pre-session)");

        //LAST SESSION END TIME
        JsonNode lastSession = sessions.get("sessions").get(sessionCount-1); //last session
        LocalTime lastSessionEnd = LocalTime.parse(lastSession.get("end_time").asText());
        LocalTime expectedEnd = LocalTime.of(21, 45);
        assertEquals(expectedEnd,lastSessionEnd,
                "Last session should end at 21:45 (opening 16:30 + 3 sessions of 1h30 with 2 breaks of 15 min + 15 min pre/post-session)");

        //REPORTER
        reporter.publishEntry("info", sessionCount + " sessions created for 16h30-22h00 hours");
    }

    @Test
    @Order(3)
    @DisplayName("One session for exact 2H window (pre 15min + session 90min + post 15min)")
    void singleSessionJson() throws Exception {
        // ARRANGE - Exact minimum: 15min pre + 90min session + 15min post = 120min = 2h
        LocalTime opening = LocalTime.of(16, 30);
        LocalTime closing = LocalTime.of(18, 30);

        // ACT
        String json = handler.generateSessionsJson(opening, closing);
        JsonNode sessions = objectMapper.readTree(json);
        int sessionCount = sessions.get("sessions").size();

        // ASSERT
        assertEquals(1, sessionCount, "Should generate exactly 1 session for 2 hour window");
        assertEquals(90, sessions.get("sessions").get(0).get("duration_minutes").asInt());

        //REPORTER
        reporter.publishEntry("info", sessionCount + " session created for 16h30-18h30 hours");
    }


    @Test
    @Order(4)
    @DisplayName("Short window that cannot fit a session")
    void tooShortWindowJson() throws Exception {
        // ARRANGE - Only Session long (no pre/post time)
        LocalTime opening = LocalTime.of(14, 0);
        LocalTime closing = LocalTime.of(15, 30);

        // ACT
        String json = handler.generateSessionsJson(opening, closing);
        JsonNode sessions = objectMapper.readTree(json);
        int sessionCount = sessions.get("sessions").size();

        // ASSERT
        assertEquals(0, sessionCount, "Should generate 0 sessions for too short window");
        assertTrue(sessions.get("sessions").isArray()); //Just double check that we at least pull the JSON

        //REPORTER
        reporter.publishEntry("info", sessionCount + " sessions created for too short window");
    }

    @Test
    @Order(5)
    @DisplayName("Large time window (9h30-19h00)")
    void largeWindowJson() throws Exception {
        // ARRANGE -- Start should be then at 10:00, 5 1H30 sessions, ending with 30 minutes (balancing to avoid long waits either pre / or long closure in post)
        LocalTime opening = LocalTime.of(9, 30);
        LocalTime closing = LocalTime.of(19, 0);


        // ACT
        String json = handler.generateSessionsJson(opening, closing);
        JsonNode sessions = objectMapper.readTree(json);
        int sessionCount = sessions.get("sessions").size();

        // ASSERT
        assertTrue(sessionCount == 5, "5 sessions should be created.");

        JsonNode firstSession = sessions.get("sessions").get(0); //firstSession

        // CHECK FIRST SESSION START TIME
        LocalTime sessionStart = LocalTime.parse(firstSession.get("start_time").asText());
        LocalTime expectedStart = LocalTime.of(10, 0);  // 9:30 + 30 min pre-session
        assertEquals(expectedStart, sessionStart,
                "First session should start at 10:00 (opening 9:30 + 30 min pre-session)");

        //LAST SESSION END TIME
        JsonNode lastSession = sessions.get("sessions").get(sessionCount-1); //last session
        LocalTime lastSessionEnd = LocalTime.parse(lastSession.get("end_time").asText());
        LocalTime expectedEnd = LocalTime.of(18, 30);
        assertEquals(expectedEnd,lastSessionEnd,
                "Last session should end at 18:30 (opening 9:30 + 5 session of 1h30 (with 15 min breaks) + 30 min pre/post-session)");

        reporter.publishEntry("info", sessionCount + " sessions created for 9h30-19h00 hours");
    }

    @Test
    @Order(5)
    @DisplayName("Weird hours test (16h10-21h59)")
    void weirdHoursJson() throws Exception {
        // ARRANGE -- Start is at 16:10, should pick 16:40 for first session and remaining time after last should be 19 minutes
        LocalTime opening = LocalTime.of(16, 10);
        LocalTime closing = LocalTime.of(21, 59);


        // ACT
        String json = handler.generateSessionsJson(opening, closing);
        JsonNode sessions = objectMapper.readTree(json);
        int sessionCount = sessions.get("sessions").size();

        // ASSERT
        assertTrue(sessionCount == 3, "3 sessions should be created.");

        JsonNode firstSession = sessions.get("sessions").get(0); //firstSession

        // CHECK FIRST SESSION START TIME
        LocalTime sessionStart = LocalTime.parse(firstSession.get("start_time").asText());
        LocalTime expectedStart = LocalTime.of(16, 40);  // 16:10 + 30 min pre-session
        assertEquals(expectedStart, sessionStart,
                "First session should start at 16:40");

        //LAST SESSION END TIME
        JsonNode lastSession = sessions.get("sessions").get(sessionCount-1); //last session
        LocalTime lastSessionEnd = LocalTime.parse(lastSession.get("end_time").asText());
        LocalTime expectedEnd = LocalTime.of(21, 40); //So 19 min before closing and still should be valid
        assertEquals(expectedEnd,lastSessionEnd,
                "Last session should end at 21:40 (opening 16:10 + 5 session of 1h30 (with 15 min breaks) + 19 min pre/post-session)");

        long leftover = java.time.temporal.ChronoUnit.MINUTES.between(lastSessionEnd, closing);

        //REPORTER
        reporter.publishEntry(
                "info",
                sessionCount + " sessions created for weird 16:10-21h59 hours. Remaining time until closure: "
                        + leftover + " minutes");
    }

    /// EXCEPTION TESTS///
    @Test
    @Order(7)
    @DisplayName("EXCEPTION - 16h00-23h00 (post-session > 30 min - not good for business!)")
    void exceptionInvalidHours() {
        LocalTime opening = LocalTime.of(16, 0);
        LocalTime closing = LocalTime.of(23, 0);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> ValidationBoiler.verifyEnoughSiteHours(opening, closing),
                "Expected validation to fail for 16:00-23:00");

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().toLowerCase().contains("invalid site hours")
                        || ex.getReason().toLowerCase().contains("too short"),
                "Error reason should mention invalid site hours");

        //REPORTER
        reporter.publishEntry("info", "Validation failed as expected for 16h00-23h00 hours: " + ex.getReason());
    }

}
