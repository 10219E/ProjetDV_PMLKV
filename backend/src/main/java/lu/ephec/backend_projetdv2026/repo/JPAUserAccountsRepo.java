package lu.ephec.backend_projetdv2026.repo;

import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.UserAccounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JPAUserAccountsRepo extends JpaRepository<UserAccounts, Integer> {

    // Find account by user id
    Optional<UserAccounts> findByUser_Matricule(String userId);

    // Find all accounts with specific status
    List<UserAccounts> findByStatus(String status);

    // Find all users with debt
    List<UserAccounts> findByStatusIgnoreCase(String status);

    // Count users with debt
    Integer countByStatus(String status);

    // Check if user has debt
    @Query("SELECT CASE WHEN COUNT(ua) > 0 THEN true ELSE false END FROM UserAccounts ua WHERE ua.user.matricule = :userId AND ua.status = 'debt'")
    boolean hasDebt(@Param("userId") String userId);

    // Get total balance owed across all users
    @Query("SELECT COALESCE(SUM(ua.balance), 0) FROM UserAccounts ua WHERE ua.status = 'debt'")
    Double getTotalDebt();

    // Get balance for specific user
    @Query("SELECT ua.balance FROM UserAccounts ua WHERE ua.user.matricule = :userId")
    Optional<Double> getBalanceByUser(@Param("userId") String userId);

    // Fetch account with user details
    @Query("SELECT ua FROM UserAccounts ua JOIN FETCH ua.user WHERE ua.user.matricule = :userId")
    Optional<UserAccounts> findByUserWithDetails(@Param("userId") String userId);

    // Update balance for user
    @Modifying
    @Transactional
    @Query("UPDATE UserAccounts ua SET ua.balance = ua.balance + :amount, ua.lastUpdate = CURRENT_TIMESTAMP WHERE ua.user.matricule = :userId")
    void updateBalanceByUser(@Param("userId") String userId, @Param("amount") Double amount);

    // Update status for user
    @Modifying
    @Transactional
    @Query("UPDATE UserAccounts ua SET ua.status = :status, ua.lastUpdate = CURRENT_TIMESTAMP WHERE ua.user.matricule = :userId")
    void updateStatusByUser(@Param("userId") String userId, @Param("status") String status);

    // Find all users in debt
    @Query("SELECT ua FROM UserAccounts ua JOIN FETCH ua.user WHERE ua.status = 'debt'")
    List<UserAccounts> findAllDebtorsWithDetails();
}
