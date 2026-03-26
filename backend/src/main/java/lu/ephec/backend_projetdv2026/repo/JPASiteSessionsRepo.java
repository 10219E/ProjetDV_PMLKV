package lu.ephec.backend_projetdv2026.repo;

import lu.ephec.backend_projetdv2026.models.SiteSessions;

import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDateTime;
import java.util.List;

public interface JPASiteSessionsRepo extends JpaRepository<SiteSessions, Integer> {


    List<SiteSessions> findByStartTime(LocalDateTime startTime);
    List<SiteSessions> findByEndTime(LocalDateTime endTime);

}