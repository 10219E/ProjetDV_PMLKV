package lu.ephec.backend_projetdv2026.repo;

import lu.ephec.backend_projetdv2026.models.Field;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JPAFieldRepo extends JpaRepository<Field, Integer> {
    List<Field> findBySite_SiteId(Integer siteId);

    List<Field> findBySite_SiteIdAndIsIndoorTrue(Integer siteId);

    List<Field> findBySite_SiteIdAndIsIndoorFalse(Integer siteId);

    List<Field> findBySite_SiteIdAndIsActiveTrue(Integer siteId);

    List<Field> findBySite_SiteIdAndIsActiveFalse(Integer siteId);

    List<Field> findBySite_SiteIdAndMaintenanceFromDateIsNotNullAndMaintenanceToDateIsNotNull(Integer siteId);

    // Count active fields (only fields with isActive = true)
    long countByIsActiveTrue();
}