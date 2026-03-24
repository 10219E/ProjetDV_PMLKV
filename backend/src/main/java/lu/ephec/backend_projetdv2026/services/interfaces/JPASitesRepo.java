package lu.ephec.backend_projetdv2026.services.interfaces;

import lu.ephec.backend_projetdv2026.models.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JPASitesRepo extends JpaRepository<Site, Integer> {
    Optional<Site> findByName(String name);
    Optional<Site> findByAddress(String address);





}