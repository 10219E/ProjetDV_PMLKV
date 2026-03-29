package lu.ephec.backend_projetdv2026.repo;

import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.UserPenalties;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

//Methods in UserService as closely related
public interface JPAUserPenaltiesRepo extends JpaRepository<UserPenalties, Integer> {

    Integer countByUserMatricule(String userId);

    @Query("SELECT p FROM UserPenalties p JOIN FETCH p.user WHERE p.user.matricule = :userId") //Fetching user info while in transacation
    List<UserPenalties> findByUserMatriculeWithUser(@Param("userId") String userId);

    @Query("SELECT p FROM UserPenalties p JOIN FETCH p.user WHERE p.isActive = true")
    List<UserPenalties> findAllActiveWithUser(String userId);

    @Query("SELECT p FROM UserPenalties p JOIN FETCH p.user WHERE LOWER(p.reason) = LOWER(:reason)")
    List<UserPenalties> findAllWithUserByReason(@Param("reason") String reason);

    List<UserPenalties> findAll();

    List<UserPenalties> findAllByIsActiveTrue();


    @Modifying
    @Transactional
    @Query("DELETE FROM UserPenalties p WHERE p.user.matricule = :userId")
    void deleteAllByUserMatricule(@Param("userId") String userId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM UserPenalties p WHERE p.user.matricule = :userId AND p.isActive = true AND :now BETWEEN p.startDate AND p.endDate")
    boolean existsActivePenaltyAt(@Param("userId") String userId, @Param("now") LocalDateTime now);

    // Find penalties for specific USER by date range
    @Query("SELECT p FROM UserPenalties p JOIN FETCH p.user WHERE p.user.matricule = :userId AND p.startDate >= :startDate AND p.endDate <= :endDate")
    List<UserPenalties> fetchPenaltiesByUserAndDateRange(@Param("userId") String userId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);


}
