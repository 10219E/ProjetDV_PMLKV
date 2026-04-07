package lu.ephec.backend_projetdv2026.repo;

import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.UsersSites;
import lu.ephec.backend_projetdv2026.models.UsersSitesId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JPAUserSiteRepo extends JpaRepository<UsersSites, UsersSitesId> {
    List<UsersSites> findBySite_SiteId(Integer siteId);

    Optional<UsersSites> findByUser_MatriculeAndSite_SiteId(String userId, Integer siteId);

    boolean existsByUser_MatriculeAndSite_SiteId(String userId, Integer siteId);

    List<UsersSites> findByUser_Matricule(String userId);

    List<UsersSites> findByIsVip(Boolean isVip);

    @Modifying
    @Transactional
    @Query("UPDATE UsersSites us SET us.isPrimary = false WHERE us.user.matricule = :userId")
    void clearPrimaryForUser(@Param("userId") String userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UsersSites us WHERE us.user.matricule = :userId AND us.site.siteId = :siteId")
    void deleteByUserAndSite(@Param("userId") String userId, @Param("siteId") Integer siteId);
}