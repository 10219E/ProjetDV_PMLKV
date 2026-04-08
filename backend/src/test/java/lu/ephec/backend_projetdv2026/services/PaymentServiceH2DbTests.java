package lu.ephec.backend_projetdv2026.services;

import com.github.javafaker.Faker;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lu.ephec.backend_projetdv2026.InitBaseH2Test;
import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.models.MatchPayments;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.models.UserRoles;
import lu.ephec.backend_projetdv2026.repo.JPAFieldRepo;
import lu.ephec.backend_projetdv2026.repo.JPAMatchPaymentsRepo;
import lu.ephec.backend_projetdv2026.repo.JPAMatchRepo;
import lu.ephec.backend_projetdv2026.repo.JPAUserRepo;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SpringBootTest
public class PaymentServiceH2DbTests extends InitBaseH2Test {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserService userService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private JPAUserRepo jpaUserRepo;

    @Autowired
    private JPAMatchRepo jpaMatchRepo;

    @Autowired
    private JPAMatchPaymentsRepo jpaMatchPaymentsRepo;

    @Autowired
    private JPAFieldRepo jpaFieldRepo;

    @PersistenceContext
    private EntityManager em;

    private TestReporter reporter;

    @BeforeEach
    void initReporter(TestReporter reporter) {
        this.reporter = reporter;
    }

    ///HELPER METHODS///
    private User createSubscribedUser() {
        User u = new User();
        u.setIsActive(true);
        u.setFirstName(Faker.instance().name().firstName());
        u.setLastName(Faker.instance().name().lastName());
        u.setEmail("paytest-" + UUID.randomUUID() + "@test.com");
        u.setBirthDate(Faker.instance().date().birthday(18, 65).toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate());
        u.setRole(em.find(UserRoles.class, (short) 1)); // subscribed
        u.setLevel("débutant");
        u.setCreated(LocalDateTime.now());
        u.setAuth(null);
        return userService.newUser(u);
    }

    private Match createPublicMatch() {
        Match m = new Match();
        m.setField(jpaFieldRepo.findAll().stream().findAny()
                .orElseThrow(() -> new RuntimeException("No field found in DB")));
        m.setType("public");
        m.setPubStatus("open");
        m.setPrivStatus(null);
        m.setMatchDate(LocalDate.now().plusDays(2));
        m.setStartTime(LocalTime.of(10, 0));
        m.setEndTime(LocalTime.of(11, 0));
        m.setOrganiser(null);
        m.setMinPlayers(2);
        m.setMaxPlayers(8);
        m.setPricing(25);
        return jpaMatchRepo.save(m);
    }

    private void cleanup(Integer paymentId, Integer matchId, String userId) {
        if (paymentId != null && jpaMatchPaymentsRepo.existsById(paymentId)) {
            jpaMatchPaymentsRepo.deleteById(paymentId);
        }
        if (matchId != null && jpaMatchRepo.existsById(matchId)) {
            matchService.deleteMatch(matchId);
        }
        if (userId != null && jpaUserRepo.existsById(userId)) {
            userService.deleteUser(userId);
        }
    }
    //////

    @Nested
    @DisplayName("CRUD - PaymentService Tests")
    class PaymentCrudTests {

        @Test
        @Order(1)
        void newPaymentClearCounterDB() {
            String userId = null;
            Integer matchId = null;
            Integer paymentId = null;

            MatchPayments saved = null;
            try {
                User user = createSubscribedUser();
                userId = user.getMatricule();

                Match match = createPublicMatch();
                matchId = match.getMatchId();

                MatchPayments p = new MatchPayments();
                p.setUser(user);
                p.setMatch(match);
                p.setAmount(25.0);
                p.setStatus("clear");
                p.setPaymentMethod("COUNTER");

                saved = paymentService.newPayment(p);
                paymentId = saved.getTr();

                assertNotNull(saved.getTr(), "Payment TR should be generated");
                assertEquals(25.0, saved.getAmount(), 0.01, "Amount should be preserved");
                assertEquals("clear", saved.getStatus(), "Status should be clear");
                assertEquals("COUNTER", saved.getPaymentMethod(), "Method should be COUNTER");
                assertNotNull(saved.getPaymentDate(), "Payment date should be auto-filled");


            } finally {
                cleanup(paymentId, matchId, userId);
                reporter.publishEntry("info", "newPaymentClearCounterDB OK | paymentId=" + saved.getTr());
            }
        }

        @Test
        @Order(2)
        void newPaymentRefundedBecomesNegativeDB() {
            String userId = null;
            Integer matchId = null;
            Integer paymentId = null;

            MatchPayments saved = null;
            try {
                User user = createSubscribedUser();
                userId = user.getMatricule();

                Match match = createPublicMatch();
                matchId = match.getMatchId();

                MatchPayments p = new MatchPayments();
                p.setUser(user);
                p.setMatch(match);
                p.setAmount(30.0);
                p.setStatus("refunded");
                p.setPaymentMethod("CARD");

                saved = paymentService.newPayment(p);
                paymentId = saved.getTr();

                assertTrue(saved.getAmount() < 0, "Refund amount must be negative");
                assertEquals(-30.0, saved.getAmount(), 0.01, "Refund should be -abs(amount)");

            } finally {
                cleanup(paymentId, matchId, userId);
                reporter.publishEntry("info", "newPaymentRefundedBecomesNegativeDB OK | paymentId=" + saved.getTr());
            }
        }

        @Test
        @Order(3)
        void updatePaymentStatusDB() {
            String userId = null;
            Integer matchId = null;
            Integer paymentId = null;

            MatchPayments saved = null;
            try {
                User user = createSubscribedUser();
                userId = user.getMatricule();

                Match match = createPublicMatch();
                matchId = match.getMatchId();

                MatchPayments p = new MatchPayments();
                p.setUser(user);
                p.setMatch(match);
                p.setAmount(20.0);
                p.setStatus("pending");
                p.setPaymentMethod("CARD");

                saved = paymentService.newPayment(p);
                paymentId = saved.getTr();

                MatchPayments update = new MatchPayments();
                update.setUser(user);
                update.setMatch(match);
                update.setStatus("failed");

                Optional<MatchPayments> updated = paymentService.updatePayment(saved.getTr(), update);

                assertTrue(updated.isPresent(), "Updated payment should be present");
                assertEquals("failed", updated.get().getStatus(), "Status should be updated to failed");

            } finally {
                cleanup(paymentId, matchId, userId);
                reporter.publishEntry("info", "updatePaymentStatusDB OK | paymentId=" + saved.getTr());
            }
        }
    }

    @Nested
    @DisplayName("EXCEPTIONS - PaymentService Tests")
    class PaymentExceptionTests {

        @Test
        @Order(1)
        void newPaymentInvalidStatusDB() {
            String userId = null;
            Integer matchId = null;

            try {
                User user = createSubscribedUser();
                userId = user.getMatricule();

                Match match = createPublicMatch();
                matchId = match.getMatchId();

                MatchPayments p = new MatchPayments();
                p.setUser(user);
                p.setMatch(match);
                p.setAmount(20.0);
                p.setStatus("unknown");
                p.setPaymentMethod("CARD");

                assertThrows(ResponseStatusException.class, () -> paymentService.newPayment(p));

            } finally {
                cleanup(null, matchId, userId);
                reporter.publishEntry("info", "newPaymentInvalidStatusDB OK | invalid status rejected");
            }
        }

        @Test
        @Order(2)
        void newPaymentInvalidMethodDB() {
            String userId = null;
            Integer matchId = null;

            try {
                User user = createSubscribedUser();
                userId = user.getMatricule();

                Match match = createPublicMatch();
                matchId = match.getMatchId();

                MatchPayments p = new MatchPayments();
                p.setUser(user);
                p.setMatch(match);
                p.setAmount(20.0);
                p.setStatus("clear");
                p.setPaymentMethod("CASH");

                assertThrows(ResponseStatusException.class, () -> paymentService.newPayment(p));

            } finally {
                cleanup(null, matchId, userId);
                reporter.publishEntry("info", "newPaymentInvalidMethodDB OK | invalid method rejected");
            }
        }

        @Test
        @Order(3)
        void newUserAccountDuplicateDB() {
            String userId = null;

            try {
                User user = createSubscribedUser();
                userId = user.getMatricule();

                final String userIdFinal = userId;
                assertThrows(ResponseStatusException.class, () -> paymentService.newUserAccount(userIdFinal));

            } finally {
                cleanup(null, null, userId);
                reporter.publishEntry("info", "newUserAccountDuplicateDB OK | duplicate account rejected");
            }
        }
    }

    @Nested
    @DisplayName("ACCOUNT - PaymentService Tests")
    class AccountTests {

        @Test
        @Order(1)
        void updateUserBalanceAndFetchDB() {
            String userId = null;

            try {
                User user = createSubscribedUser();
                userId = user.getMatricule();

                paymentService.updateUserBalance(userId, 42.5);
                Double balance = paymentService.fetchUserBalance(userId).orElseThrow();

                assertEquals(42.5, balance, 0.01, "Balance should be updated");

            } finally {
                cleanup(null, null, userId);
                reporter.publishEntry("info", "updateUserBalanceAndFetchDB OK | balance=42.5");
            }
        }

        @Test
        @Order(2)
        void updateAccountStatusInvalidDB() {
            String userId = null;

            try {
                User user = createSubscribedUser();
                userId = user.getMatricule();

                final String userIdFinal = userId;
                assertThrows(ResponseStatusException.class,
                        () -> paymentService.updateAccountStatus(userIdFinal, "invalid_status", 10.0));

            } finally {
                cleanup(null, null, userId);
                reporter.publishEntry("info", "updateAccountStatusInvalidDB OK | invalid status rejected");
            }
        }
    }
}
