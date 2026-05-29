package lu.ephec.backend_projetdv2026.repo;

import lu.ephec.backend_projetdv2026.models.MatchPayments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JPAMatchPaymentsRepo extends JpaRepository<MatchPayments, Integer> {

    // Find all payments for a specific user
    List<MatchPayments> findByUser_Matricule(String userId);

    // Find all payments for a specific match
    List<MatchPayments> findByMatch_MatchId(Integer matchId);

    // Find payments by status
    List<MatchPayments> findByStatus(String status);

    // Find all pending payments
    List<MatchPayments> findByStatusIgnoreCase(String status);

    // Find payments for a user in a specific match
    Optional<MatchPayments> findByUser_MatriculeAndMatch_MatchId(String userId, Integer matchId);

    // Find payments by payment method
    List<MatchPayments> findByPaymentMethod(String paymentMethod);

    // Find payments within a date range
    List<MatchPayments> findByPaymentDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Find payments for a user with specific status
    List<MatchPayments> findByUser_MatriculeAndStatus(String userId, String status);

    // Custom query to fetch payments with user and match info
    @Query("SELECT p FROM MatchPayments p JOIN FETCH p.user JOIN FETCH p.match WHERE p.user.matricule = :userId")
    List<MatchPayments> findByUserWithDetails(@Param("userId") String userId);

    // Calculate total amount paid by user
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM MatchPayments p WHERE p.user.matricule = :userId AND p.status = 'clear'")
    Double getTotalPaidByUser(@Param("userId") String userId);

    // Calculate total amount for a match
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM MatchPayments p WHERE p.match.matchId = :matchId AND p.status = 'clear'")
    Double getTotalPaidForMatch(@Param("matchId") Integer matchId);

    // Count pending payments
    Integer countByStatus(String status);

    //Fetch all payments ordered by date descending, including site and user information.
    @Query("SELECT p FROM MatchPayments p " +
           "JOIN FETCH p.match m " +
           "JOIN FETCH m.field f " +
           "JOIN FETCH f.site s " +
           "JOIN FETCH p.user u " +
           "WHERE p.status = 'clear' " +
           "ORDER BY p.paymentDate DESC")
    List<MatchPayments> findAllClearedPaymentsWithDetails();

    //Fetch all payments for a specific site, ordered by date descending including match, field, site and user details.
    @Query("SELECT p FROM MatchPayments p " +
           "JOIN FETCH p.match m " +
           "JOIN FETCH m.field f " +
           "JOIN FETCH f.site s " +
           "JOIN FETCH p.user u " +
           "WHERE p.status = 'clear' AND s.siteId = :siteId " +
           "ORDER BY p.paymentDate DESC")
    List<MatchPayments> findAllClearedPaymentsBySiteWithDetails(@Param("siteId") Integer siteId);
}