package lu.ephec.backend_projetdv2026.services;

import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.MatchPayments;
import lu.ephec.backend_projetdv2026.models.UserAccounts;
import lu.ephec.backend_projetdv2026.repo.JPAMatchPaymentsRepo;
import lu.ephec.backend_projetdv2026.repo.JPAUserAccountsRepo;
import lu.ephec.backend_projetdv2026.services.validation.ValidationBoiler;
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

    // Dependency Injection
    public PaymentService(JPAMatchPaymentsRepo jpaMatchPaymentsRepo, JPAUserAccountsRepo jpaUserAccountsRepo) {
        this.jpaMatchPaymentsRepo = jpaMatchPaymentsRepo;
        this.jpaUserAccountsRepo = jpaUserAccountsRepo;
    }

    /// MATCH PAYMENTS ///
    // ADD Payment
    @Transactional
    public MatchPayments addPayment(MatchPayments payment) {
        ValidationBoiler.verifyNotNull(payment, "Payment");
        ValidationBoiler.verifyNotNull(payment.getAmount(), "Payment amount");
        ValidationBoiler.verifyNotEmpty(payment.getStatus(), "Payment status");
        ValidationBoiler.verifyNotEmpty(payment.getPaymentMethod(), "Payment method");

        // Validate status
        if (!payment.getStatus().matches("^(clear|pending|cancelled|failed|refunded)$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid payment status. Must be clear, pending, cancelled, failed, or refunded. Received: " + payment.getStatus());
        }

        // Validate payment method
        if (!payment.getPaymentMethod().matches("^(COUNTER|CARD)$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid payment method. Must be COUNTER or CARD. Received: " + payment.getPaymentMethod());
        }

        // Set payment date if not provided
        if (payment.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }

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

    // GET Transactions for One Match
    public List<MatchPayments> fetchByMatch(Integer matchId) {
        ValidationBoiler.verifyNotNull(matchId, "Match ID");

        List<MatchPayments> payments = jpaMatchPaymentsRepo.findByMatch_MatchId(matchId);
        if (payments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No payments found for match: " + matchId);
        }
        return payments;
    }

    // GET Transactions for a User
    public List<MatchPayments> fetchByUser(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");

        List<MatchPayments> payments = jpaMatchPaymentsRepo.findByUser_Matricule(userId);
        if (payments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No payments found for user: " + userId);
        }
        return payments;
    }

    // GET Transactions Within a Date Range
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

    // GET Transactions by Payment Method (Type)
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

    // GET Transactions by Status
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

    // UPDATE Match Payment
    @Transactional
    public Optional<MatchPayments> updatePayment(Integer paymentId, MatchPayments updatedPayment) {
        ValidationBoiler.verifyNotNull(paymentId, "Payment ID");
        ValidationBoiler.verifyNotNull(updatedPayment, "Update data");
        ValidationBoiler.verifyExists(jpaMatchPaymentsRepo.existsById(paymentId), "Payment", paymentId);

        return jpaMatchPaymentsRepo.findById(paymentId).map(payment -> {
            // Update amount if provided
            if (updatedPayment.getAmount() != null) {
                payment.setAmount(updatedPayment.getAmount());
            }

            // Update status if provided
            if (updatedPayment.getStatus() != null) {
                if (!updatedPayment.getStatus().matches("^(clear|pending|cancelled|failed|refunded)$")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid payment status. Must be clear, pending, cancelled, failed, or refunded. Received: " + updatedPayment.getStatus());
                }
                payment.setStatus(updatedPayment.getStatus());
            }

            // Update payment method if provided
            if (updatedPayment.getPaymentMethod() != null) {
                if (!updatedPayment.getPaymentMethod().matches("^(COUNTER|CARD)$")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid payment method. Must be COUNTER or CARD. Received: " + updatedPayment.getPaymentMethod());
                }
                payment.setPaymentMethod(updatedPayment.getPaymentMethod());
            }

            // Update payment date if provided
            if (updatedPayment.getPaymentDate() != null) {
                payment.setPaymentDate(updatedPayment.getPaymentDate());
            }

            return jpaMatchPaymentsRepo.save(payment);
        });
    }


    /// USER ACCOUNTS (FINANCIAL STATUS) ////


    // FETCH Balance for User
    public Optional<Double> fetchUserBalance(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");

        Optional<Double> balance = jpaUserAccountsRepo.getBalanceByUser(userId);
        if (balance.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No account found for user: " + userId);
        }
        return balance;
    }

    // FETCH User Account with Details
    public Optional<UserAccounts> fetchUserAccountWithDetails(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");

        Optional<UserAccounts> account = jpaUserAccountsRepo.findByUserWithDetails(userId);
        if (account.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No account found for user: " + userId);
        }
        return account;
    }

    // UPDATE Balance for User
    @Transactional
    public void updateUserBalance(String userId, Double amount) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyNotNull(amount, "Amount");

        // Check if account exists
        Optional<UserAccounts> account = jpaUserAccountsRepo.findByUser_Matricule(userId);
        if (account.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No account found for user: " + userId);
        }

        jpaUserAccountsRepo.updateBalanceByUser(userId, amount);
    }

    // FETCH ALL User Accounts
    public List<UserAccounts> fetchAllUserAccounts() {
        List<UserAccounts> accounts = jpaUserAccountsRepo.findAll();
        if (accounts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No user accounts found");
        }
        return accounts;
    }

    // FETCH User Accounts by Status (clear or debt)
    public List<UserAccounts> fetchByStatusForUser(String userId, String status) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyNotEmpty(status, "Status");

        if (!status.matches("^(clear|debt)$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status. Must be clear or debt. Received: " + status);
        }

        Optional<UserAccounts> account = jpaUserAccountsRepo.findByUser_Matricule(userId);
        if (account.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No account found for user: " + userId);
        }

        // Return single account wrapped in list if status matches
        if (account.get().getStatus().equals(status)) {
            return List.of(account.get());
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "User " + userId + " does not have status: " + status);
        }
    }

    // FETCH ALL Debtors
    public List<UserAccounts> fetchAllDebtors() {
        List<UserAccounts> debtors = jpaUserAccountsRepo.findAllDebtorsWithDetails();
        if (debtors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No debtors found");
        }
        return debtors;
    }

    // COUNT Debtors
    public Integer countDebtors() {
        Integer count = jpaUserAccountsRepo.countByStatus("debt");
        return count != null ? count : 0;
    }

    // CALCULATE Total Debt
    public Double calculateTotalDebt() {
        Double totalDebt = jpaUserAccountsRepo.getTotalDebt();
        return totalDebt != null ? totalDebt : 0.0;
    }

    // CHECK if User Has Debt
    public boolean userHasDebt(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        return jpaUserAccountsRepo.hasDebt(userId);
    }

    // UPDATE Account Status
    @Transactional
    public void updateAccountStatus(String userId, String status) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyNotEmpty(status, "Status");

        if (!status.matches("^(clear|debt)$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid status. Must be clear or debt. Received: " + status);
        }

        Optional<UserAccounts> account = jpaUserAccountsRepo.findByUser_Matricule(userId);
        if (account.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No account found for user: " + userId);
        }

        jpaUserAccountsRepo.updateStatusByUser(userId, status);
    }
}
