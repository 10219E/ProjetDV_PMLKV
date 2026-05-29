package lu.ephec.backend_projetdv2026.repo;

import lu.ephec.backend_projetdv2026.models.UsersBookingRules;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JPAUserBookingRepo extends JpaRepository<UsersBookingRules, Short> {

    @Query("SELECT b.allowedDuration FROM UsersBookingRules b WHERE b.roleId = :roleId")
    Optional<Integer> findAllowedDurationByRoleId(Short roleId);
}
