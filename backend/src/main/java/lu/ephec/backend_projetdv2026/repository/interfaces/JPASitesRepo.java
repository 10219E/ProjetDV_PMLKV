package lu.ephec.backend_projetdv2026.repository.interfaces;

import lu.ephec.backend_projetdv2026.models.Site;
import lu.ephec.backend_projetdv2026.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface JPASitesRepo extends JpaRepository<Site, Integer> {
    Optional<Site> findByName(String name);
    Optional<Site> findByAddress(String address);





}