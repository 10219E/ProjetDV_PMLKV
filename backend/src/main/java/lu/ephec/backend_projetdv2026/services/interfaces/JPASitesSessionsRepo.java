package lu.ephec.backend_projetdv2026.services.interfaces;

import lu.ephec.backend_projetdv2026.models.SiteSessions;

import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDateTime;
import java.util.List;

public interface JPASitesSessionsRepo extends JpaRepository<SiteSessions, Integer> {


    List<SiteSessions> findByStartTime(LocalDateTime startTime);
    List<SiteSessions> findByEndTime(LocalDateTime endTime);

}