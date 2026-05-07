package lu.ephec.backend_projetdv2026.services;

import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.MatchPayments;
import lu.ephec.backend_projetdv2026.models.UserAccounts;
import lu.ephec.backend_projetdv2026.repo.JPAMatchPaymentsRepo;
import lu.ephec.backend_projetdv2026.repo.JPAMatchRepo;
import lu.ephec.backend_projetdv2026.repo.JPAUserAccountsRepo;
import lu.ephec.backend_projetdv2026.repo.JPAUserRepo;
import lu.ephec.backend_projetdv2026.services.validation.ValidationBoiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final JPAMatchPaymentsRepo jpaMatchPaymentsRepo;
    private final JPAUserAccountsRepo jpaUserAccountsRepo;
    private final JPAMatchRepo jpaMatchRepo;
    private final JPAUserRepo jpaUserRepo;
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    // Dependency Injection
    public PaymentService(JPAMatchPaymentsRepo jpaMatchPaymentsRepo, JPAUserAccountsRepo jpaUserAccountsRepo, JPAMatchRepo jpaMatchRepo, JPAUserRepo jpaUserRepo) {
        this.jpaMatchPaymentsRepo = jpaMatchPaymentsRepo;
        this.jpaUserAccountsRepo = jpaUserAccountsRepo;
        this.jpaMatchRepo = jpaMatchRepo;
        this.jpaUserRepo = jpaUserRepo;
    }

    /// MATCH PAYMENTS ///
    
    // SET PAYMENT MATCH
    @Transactional
    public MatchPayments newPayment(MatchPayments payment) {
        logger.info("[Service - Payment Service] Creating new payment: {}", payment);
        ValidationBoiler.verifyNotNull(payment, "Payment");
        ValidationBoiler.verifyNotNull(payment.getAmount(), "Payment amount");
        ValidationBoiler.verifyNotEmpty(payment.getStatus(), "Payment status");

        ValidationBoiler.verifyExists(jpaUserRepo.existsById(payment.getUser().getMatricule()), "User", payment.getUser().getMatricule());
        ValidationBoiler.verifyExists(jpaMatchRepo.existsById(payment.getMatch().getMatchId()), "Match", payment.getMatch().getMatchId());

        // Validate status (creation: cancelled not applicable here)
        if (!payment.getStatus().matches("^(clear|pending|failed|refunded)$")) {
            logger.error("[Service - Payment Service] Invalid payment status: {}", payment.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid payment status. Must be clear, pending, failed or refunded. Received: " + payment.getStatus());
        }

        // If refunded, apply negative amount
        if (payment.getStatus().equals("refunded")) {
            payment.setAmount(-Math.abs(payment.getAmount()));
        }

        String method = payment.getPaymentMethod(); // may be null
        LocalDateTime pDate = payment.getPaymentDate();
        String status = payment.getStatus();

        // Validate payment method when present
        if (method != null && !method.matches("^(COUNTER|CARD)$")) {
            logger.error("[Service - Payment Service] Invalid payment method: {}", method);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid payment method. Must be COUNTER or CARD when provided. Received: " + method);
        }

        // Business rules:
        // - If status is 'clear' or 'refunded' then payment_date AND payment_method must be present.
        // - If payment_date is provided, payment_method must also be present.
        // - payment_method may be NULL only when status is NOT 'clear'/'refunded' AND payment_date IS NULL.

        logger.info("[Service - Payment Service] Validating payment status and method: status={}, method={}", status, method);
        if (status.equals("clear") || status.equals("refunded")) {
            // ensure payment date exists (set to now if missing) and method provided
            if (pDate == null) {
                payment.setPaymentDate(LocalDateTime.now()); //we will set the date for ease as it should always be equal to payment date
                pDate = payment.getPaymentDate();
            }
            if (method == null || method.isBlank()) {
                logger.error("[Service - Payment Service] Payment method is required when status is 'clear' or 'refunded'.");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Payment method is required when status is 'clear' or 'refunded'.");
            }
        } else {
            // status not clear/refunded
            if (pDate != null && method == null) {
                // date provided but method missing -> invalid
                logger.error("[Service - Payment Service] Payment method is required when payment date is present.");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Payment method must be provided when payment date is present.");
            }
        }

        logger.info("[Service - Payment Service] Payment processed");
        return jpaMatchPaymentsRepo.save(payment);
    }

    // GET ALL Transactions
    public List<MatchPayments> fetchAll() {
        List<MatchPayments> payments = jpaMatchPaymentsRepo.findAll();
        if (payments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No payment transactions found");
        }
        return payments;
    }

    // GET TR FOR ONE MATCH
    public List<MatchPayments> fetchByMatch(Integer matchId) {
        ValidationBoiler.verifyNotNull(matchId, "Match ID");
        ValidationBoiler.verifyExists(jpaMatchRepo.existsById(matchId), "Match", matchId);

        List<MatchPayments> payments = jpaMatchPaymentsRepo.findByMatch_MatchId(matchId);
        if (payments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No payments found for match: " + matchId);
        }
        return payments;
    }

    // GET TR FOR USER
    public List<MatchPayments> fetchByUser(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);

        List<MatchPayments> payments = jpaMatchPaymentsRepo.findByUser_Matricule(userId);
        if (payments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No payments found for user: " + userId);
        }
        return payments;
    }

    // GET TR DATE RANGE
    public List<MatchPayments> fetchByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        ValidationBoiler.verifyNotNull(startDate, "Start date");
        ValidationBoiler.verifyNotNull(endDate, "End date");
        ValidationBoiler.verifyDatesValid(startDate, endDate, "Date range");

        List<MatchPayments> payments = jpaMatchPaymentsRepo.findByPaymentDateBetween(startDate, endDate);
        if (payments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No payments found within date range: " + startDate + " to " + endDate);
        }
        return payments;
    }

    // GET TR BY PAYMENT METHOD
    public List<MatchPayments> fetchByPaymentMethod(String paymentMethod) {
        ValidationBoiler.verifyNotEmpty(paymentMethod, "Payment method");

        if (!paymentMethod.matches("^(COUNTER|CARD)$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid payment method. Must be COUNTER or CARD. Received: " + paymentMethod);
        }

        List<MatchPayments> payments = jpaMatchPaymentsRepo.findByPaymentMethod(paymentMethod);
        if (payments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No payments found for method: " + paymentMethod);
        }
        return payments;
    }

    // GET TR BY STATUS
    public List<MatchPayments> fetchByStatus(String status) {
        ValidationBoiler.verifyNotEmpty(status, "Payment status");

        if (!status.matches("^(clear|pending|cancelled|failed|refunded)$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid payment status. Must be clear, pending, cancelled, failed, or refunded. Received: " + status);
        }

        List<MatchPayments> payments = jpaMatchPaymentsRepo.findByStatus(status);
        if (payments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No payments found with status: " + status);
        }
        return payments;
    }

    // GET PENDING PAYMENTS FOR A USER
    public List<MatchPayments> fetchPendingByUser(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);

        List<MatchPayments> payments = jpaMatchPaymentsRepo.findByUser_MatriculeAndStatus(userId, "pending");
        if (payments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No pending payments found for user: " + userId);
        }
        return payments;
    }

    // UPDATE MATCH PAYMENT
    @Transactional
    public Optional<MatchPayments> updatePayment(Integer paymentId, MatchPayments updatedPayment) {
        logger.info("[Service - Payment Service] Updating payment with ID: {}", paymentId);
        ValidationBoiler.verifyNotNull(paymentId, "Payment ID");
        ValidationBoiler.verifyNotNull(updatedPayment, "Update data");
        ValidationBoiler.verifyExists(jpaMatchPaymentsRepo.existsById(paymentId), "Payment", paymentId);

        // Only validate referenced Match/User if provided in the update payload to avoid NPEs
        if (updatedPayment.getMatch() != null && updatedPayment.getMatch().getMatchId() != null) {
            ValidationBoiler.verifyExists(jpaMatchRepo.existsById(updatedPayment.getMatch().getMatchId()), "Match", updatedPayment.getMatch().getMatchId());
        }

        if (updatedPayment.getUser() != null && updatedPayment.getUser().getMatricule() != null && !updatedPayment.getUser().getMatricule().isBlank()) {
            ValidationBoiler.verifyExists(jpaUserRepo.existsById(updatedPayment.getUser().getMatricule()), "User", updatedPayment.getUser().getMatricule());
        }

        return jpaMatchPaymentsRepo.findById(paymentId).map(payment -> {
            // UPDATE AMOUNT, ONLY FOR REFUNDS
            if (updatedPayment.getAmount() != null) {
                payment.setAmount(updatedPayment.getAmount());
            }

            // UPDATE STATUS
            if (updatedPayment.getStatus() != null) {
                if (!updatedPayment.getStatus().matches("^(clear|pending|cancelled|failed|refunded)$")) {
                    logger.error("[Service - Payment Service] Invalid payment status: {}", updatedPayment.getStatus());
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid payment status. Must be clear, pending, cancelled, failed, or refunded. Received: " + updatedPayment.getStatus());
                }
                payment.setStatus(updatedPayment.getStatus());
            }

            // UPDATE PAYMENT METHOD or DATE: permitted
            String newMethod = updatedPayment.getPaymentMethod(); // may be null (means no change)
            LocalDateTime newDate = updatedPayment.getPaymentDate(); // may be null (no change)

            // Validate new payment method when provided
            if (newMethod != null && !newMethod.matches("^(COUNTER|CARD)$")) {
                logger.error("[Service - Payment Service] Invalid payment method: {}", newMethod);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid payment method. Must be COUNTER or CARD when provided. Received: " + newMethod);
            }

            // Determine resulting status after update (could be unchanged)
            String resultingStatus = updatedPayment.getStatus() != null ? updatedPayment.getStatus() : payment.getStatus();

            // If a payment date is provided without a method (either new or existing), reject
            if (newDate != null) {
                String methodToUse = newMethod != null ? newMethod : payment.getPaymentMethod();
                if (methodToUse == null) {
                    logger.error("[Service - Payment Service] Payment method is required when payment date is present.");
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Payment method must be provided when payment date is present.");
                }
            }

            logger.info("[Service - Payment Service] Validating payment status and method for update: resultingStatus={}, newMethod={}, newDate={}", resultingStatus, newMethod, newDate);
            // If resulting status requires a date/method (clear or refunded), ensure they exist/apply defaults
            if ("clear".equals(resultingStatus) || "refunded".equals(resultingStatus)) {
                // Ensure method exists either from update or current
                String methodToUse = newMethod != null ? newMethod : payment.getPaymentMethod();
                if (methodToUse == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Payment method is required when status is 'clear' or 'refunded'.");
                }

                // Ensure date exists: prefer newDate, else existing, else set now
                if (newDate != null) {
                    payment.setPaymentDate(newDate);
                } else if (payment.getPaymentDate() == null) {
                    payment.setPaymentDate(LocalDateTime.now());
                }
            } else {
                // For non-clear/refunded statuses, if newDate provided, set it (method presence already validated above)
                if (newDate != null) {
                    payment.setPaymentDate(newDate);
                }
            }

            // Finally apply payment method update if provided
            if (newMethod != null) {
                payment.setPaymentMethod(newMethod);
            }

            logger.info("[Service - Payment Service] Payment processed");
            return jpaMatchPaymentsRepo.save(payment);
        });
    }


    /// USER ACCOUNTS (FINANCIAL STATUS) ////


    // NEW USER ACCOUNT (FINANCE)
    @Transactional
    public UserAccounts newUserAccount(String userId) {
        logger.info("[Service - Payment : Account] Creating new wallet for user: {}", userId);
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        ValidationBoiler.verifyUserActive(jpaUserRepo.findById(userId).orElseThrow().getIsActive(), userId);

        // Check if account already exists
        Optional<UserAccounts> existingAccount = jpaUserAccountsRepo.findByUser_Matricule(userId);
        if (existingAccount.isPresent()) {
            logger.error("[Service - Payment : Account] Wallet already exists for user: {}", userId);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Account already exists for user: " + userId);
        }

        // Create new account with defaults
        UserAccounts newAccount = new UserAccounts();
        newAccount.setUser(jpaUserRepo.findById(userId).orElseThrow());
        newAccount.setBalance(0.0);
        newAccount.setStatus("clear");
        newAccount.setLastUpdate(LocalDateTime.now());

        logger.info("[Service - Payment : Account] Wallet created for user: {}", userId);
        return jpaUserAccountsRepo.save(newAccount);
    }


    // GET BALANCE FOR USER
    public Optional<Double> fetchUserBalance(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);

        Optional<Double> balance = jpaUserAccountsRepo.getBalanceByUser(userId);
        if (balance.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No payment account found for user: " + userId);
        }
        return balance;
    }

    // FETCH USER ACCOUNT
    public Optional<UserAccounts> fetchUserAccount(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);

        Optional<UserAccounts> account = jpaUserAccountsRepo.findByUser_Matricule(userId);
        if (account.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No account found for user: " + userId);
        }
        return account;
    }

    // FETCH USER ACCOUNT WITH DETAILS
    public Optional<UserAccounts> fetchUserAccountWithDetails(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);

        Optional<UserAccounts> account = jpaUserAccountsRepo.findByUserWithDetails(userId);
        if (account.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No account found for user: " + userId);
        }
        return account;
    }

    // UPDATE BALANCE FOR USER
    @Transactional
    public void updateUserBalance(String userId, Double amount) {
        logger.info("[Service - Payment : Account] Updating balance for user: {}", userId);
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyNotNull(amount, "Amount");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);

        // Check if account exists
        Optional<UserAccounts> accountOpt = jpaUserAccountsRepo.findByUser_Matricule(userId);
        if (accountOpt.isEmpty()) {
            logger.error("[Service - Payment : Account] No wallet found for user: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No account found for user: " + userId);
        }

        // Append the passed amount to the existing balance
        UserAccounts account = accountOpt.get();
        Double currentBalance = account.getBalance() != null ? account.getBalance() : 0.0;
        account.setBalance(currentBalance + amount);
        account.setLastUpdate(LocalDateTime.now());

        logger.info("[Service - Payment : Account] Balance updated for user: {}", userId);
        jpaUserAccountsRepo.save(account);
    }

    // UPDATE ACCOUNT STATUS
    @Transactional
    public void updateAccountStatus(String userId, String status, Double amount) {
        logger.info("[Service - Payment : Account] Updating wallet status for user: {}", userId);
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyNotEmpty(status, "Status");

        if (!status.matches("^(clear|debt)$")) {
            logger.error("[Service - Payment : Account] Invalid status: {}", status);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status. Must be clear or debt. Received: " + status);
        }

        Optional<UserAccounts> accountOpt = jpaUserAccountsRepo.findByUser_Matricule(userId);
        if (accountOpt.isEmpty()) {
            logger.error("[Service - Payment : Account] No wallet found for user: {}", userId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No account found for user: " + userId);
        }

        // Append amount to existing balance when provided, then set status
        UserAccounts account = accountOpt.get();
        if (amount != null) {
            Double currentBalance = account.getBalance() != null ? account.getBalance() : 0.0;
            account.setBalance(currentBalance + amount);
        }
        account.setStatus(status);
        account.setLastUpdate(LocalDateTime.now());

        logger.info("[Service - Payment : Account] Status updated for user: {}", userId);
        jpaUserAccountsRepo.save(account);
    }

    // FETCH ALL ACCOUNTS
    public List<UserAccounts> fetchAllUserAccounts() {
        List<UserAccounts> accounts = jpaUserAccountsRepo.findAll();
        if (accounts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No user accounts found");
        }
        return accounts;
    }


    // FETCH ALL DEBTORS
    public List<UserAccounts> fetchAllDebtors() {
        List<UserAccounts> debtors = jpaUserAccountsRepo.findAllDebtorsWithDetails();
        if (debtors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No debtors found");
        }
        return debtors;
    }

    // COUNT DEBTORS
    public Integer countDebtors() {
        Integer count = jpaUserAccountsRepo.countByStatus("debt");
        return count != null ? count : 0;
    }

    // CALC TOTAL DEBT
    public Double calculateTotalDebt() {
        Double totalDebt = jpaUserAccountsRepo.getTotalDebt();
        return totalDebt != null ? totalDebt : 0.0;
    }

    // CHECK IF USER HAS DEBT
    public boolean userHasDebt(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        return jpaUserAccountsRepo.hasDebt(userId);
    }

}
