package lu.ephec.backend_projetdv2026.repo;

import lu.ephec.backend_projetdv2026.models.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface JPASiteRepo extends JpaRepository<Site, Integer> {
    Optional<Site> findByName(String name);
    Optional<Site> findByAddress(String address);

    List<Site> findByOpeningTime(LocalTime openingTime);
    List <Site> findByClosingTime(LocalTime closingTime);





}