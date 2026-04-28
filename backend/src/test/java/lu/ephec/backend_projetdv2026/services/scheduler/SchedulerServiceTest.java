package lu.ephec.backend_projetdv2026.services.scheduler;

import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.models.MatchPlayers;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.repo.JPAMatchPlayersRepo;
import lu.ephec.backend_projetdv2026.repo.JPAMatchRepo;
import lu.ephec.backend_projetdv2026.repo.JPAUserRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Disabled("If I have time")
@ActiveProfiles("test")
public class SchedulerServiceTest {

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private JPAMatchRepo jpaMatchRepo;

    @Autowired
    private JPAMatchPlayersRepo jpaMatchPlayersRepo;

    @Autowired
    private JPAUserRepo jpaUserRepo;

    @AfterEach
    void cleanup() {
        // Clean up repositories to keep tests isolated
        jpaMatchPlayersRepo.deleteAll();
        jpaMatchRepo.deleteAll();
        jpaUserRepo.deleteAll();
    }

    @Test
    public void whenMatchCompleted_thenMatchPlayersDeleted() {
        // Create user
        User u = new User();
        u.setMatricule("TST1");
        u.setIsActive(true);
        u.setFirstName("Test");
        u.setLastName("User");
        u.setEmail("test1@example.com");
        jpaUserRepo.save(u);

        // Create match that already ended
        Match match = new Match();
        match.setType("public");
        match.setPubStatus("confirmed");
        match.setMatchDate(LocalDate.now());
        match.setStartTime(LocalTime.now().minusHours(3));
        match.setEndTime(LocalTime.now().minusHours(1));
        match.setMinPlayers(4);
        match.setMaxPlayers(4);
        match.setPricing(60);
        jpaMatchRepo.save(match);

        // Create a MatchPlayers entry linked to the match
        MatchPlayers mp = new MatchPlayers();
        mp.setMatch(match);
        mp.setUser(u);
        mp.setPlayerRole("p2");
        mp.setStatus("approved");
        jpaMatchPlayersRepo.save(mp);

        // Sanity check: player exists
        List<MatchPlayers> before = jpaMatchPlayersRepo.findByMatch_MatchId(match.getMatchId());
        assertFalse(before.isEmpty(), "MatchPlayers should exist before scheduler runs");

        // Call the protected processMatchBatch via package-private access
        int updated = schedulerService.processMatchBatch(Collections.singletonList(match), LocalDateTime.now());
        assertTrue(updated >= 1, "At least one match should be processed");

        // After processing, players for that match should be deleted
        List<MatchPlayers> after = jpaMatchPlayersRepo.findByMatch_MatchId(match.getMatchId());
        assertTrue(after.isEmpty(), "MatchPlayers should be deleted after match completed");
    }

    @Test
    public void whenPrivateMatchConverted_thenOldMatchPlayersDeleted() {
        // Create user
        User u = new User();
        u.setMatricule("TST2");
        u.setIsActive(true);
        u.setFirstName("Test2");
        u.setLastName("User2");
        u.setEmail("test2@example.com");
        jpaUserRepo.save(u);

        // Create private match scheduled for tomorrow
        Match match = new Match();
        match.setType("private");
        match.setPrivStatus("awaiting");
        match.setMatchDate(LocalDate.now().plusDays(1));
        match.setStartTime(LocalTime.of(10, 0));
        match.setEndTime(LocalTime.of(11, 0));
        match.setMinPlayers(4);
        match.setMaxPlayers(4);
        match.setPricing(60);
        jpaMatchRepo.save(match);

        // Add a player to the private match
        MatchPlayers mp = new MatchPlayers();
        mp.setMatch(match);
        mp.setUser(u);
        mp.setPlayerRole("p1");
        mp.setStatus("approved");
        jpaMatchPlayersRepo.save(mp);

        // Sanity check
        List<MatchPlayers> before = jpaMatchPlayersRepo.findByMatch_MatchId(match.getMatchId());
        assertFalse(before.isEmpty(), "MatchPlayers should exist before conversion");

        // Call conversion method directly
        int created = schedulerService.processPrivateMatchConversion(match);
        assertEquals(1, created);

        // Old match players should be deleted
        List<MatchPlayers> after = jpaMatchPlayersRepo.findByMatch_MatchId(match.getMatchId());
        assertTrue(after.isEmpty(), "Old MatchPlayers should be deleted after private->public conversion");
    }
}

