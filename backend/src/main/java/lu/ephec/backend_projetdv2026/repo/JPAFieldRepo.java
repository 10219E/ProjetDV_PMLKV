package lu.ephec.backend_projetdv2026.repo;

import lu.ephec.backend_projetdv2026.models.Field;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JPAFieldRepo extends JpaRepository<Field, Integer> {
    List<Field> findBySiteId(Integer siteId);
    List<Field> findByIsIndoorTrue();
    List<Field> findByIsIndoorFalse();

    List<Field> findBySiteIdAndIsActiveTrue(Integer siteId);

    List<Field> findBySiteIdAndIsActiveFalse(Integer siteId);

    List<Field> findBySiteIdAndMaintenanceFromDateIsNotNullAndMaintenanceToDateIsNotNull(Integer siteId);
}