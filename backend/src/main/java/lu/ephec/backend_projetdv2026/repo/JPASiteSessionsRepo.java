package lu.ephec.backend_projetdv2026.repo;

import lu.ephec.backend_projetdv2026.models.SiteSessions;

import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JPASiteSessionsRepo extends JpaRepository<SiteSessions, Integer> {

    boolean existsBySite_SiteId(Integer siteId);
    Optional<SiteSessions> findBySite_SiteId(Integer siteId);

}