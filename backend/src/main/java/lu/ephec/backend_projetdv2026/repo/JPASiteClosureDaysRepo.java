package lu.ephec.backend_projetdv2026.repo;

import lu.ephec.backend_projetdv2026.models.SiteClosureDays;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface JPASiteClosureDaysRepo extends JpaRepository<SiteClosureDays, Integer> {
    List<SiteClosureDays> findBySiteId(Integer siteId);

    boolean existsBySiteIdAndClosureDate(Integer siteId, LocalDate closureDate);

    boolean existsByForAllAndClosureDate(boolean forAll, LocalDate closureDate);

    Optional<SiteClosureDays> findBySiteIdAndClosureDate(Integer siteId, LocalDate closureDate);

    List<SiteClosureDays> findByClosureDate(LocalDate closureDate);

    List<SiteClosureDays> findByClosureDateBetween(LocalDate startDate, LocalDate endDate);

}
