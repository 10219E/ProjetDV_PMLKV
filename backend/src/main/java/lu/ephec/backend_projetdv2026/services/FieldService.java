package lu.ephec.backend_projetdv2026.services;

import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.repo.JPAFieldRepo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FieldService {

    private final JPAFieldRepo jpaFieldRepo;

    // InjDep Interface Field
    public FieldService(JPAFieldRepo jpaFieldRepo) { this.jpaFieldRepo = jpaFieldRepo;}

    // SET Field
    public Field newField(Field field) {
        return jpaFieldRepo.save(field);
    }

    // GET Field by ID
    public Optional<Field> fetchById(Integer fieldId) {
        return jpaFieldRepo.findById(fieldId);
    }

    // GET Fields by Site
    public List<Field> fetchBySite(Integer siteId) {
        return jpaFieldRepo.findBySiteId(siteId);
    }

    // GET ALL Fields
    public List<Field> fetchAll() {
        return jpaFieldRepo.findAll();
    }

    //GET ALL indoor Fields
    public List<Field> fetchIndoor() {
        return jpaFieldRepo.findByIsIndoorTrue();
    }

    //GET ALL outdoor Fields
    public List<Field> fetchOutdoor() {
        return jpaFieldRepo.findByIsIndoorFalse();
    }

    // DELETE Field
    public void deleteField(Integer fieldId) {
        jpaFieldRepo.deleteById(fieldId);
    }

    // UPDATE Field
    public Optional<Field> updField(Integer fieldId, Field updateData) {
        return jpaFieldRepo.findById(fieldId).map(field -> {
            if (updateData.getIsIndoor() != null) {
                field.setIsIndoor(updateData.getIsIndoor());
            }
            if (updateData.getIsActive() != null) {
                field.setIsActive(updateData.getIsActive());
            }
            if (updateData.getMaintenanceFromDate() != null) {
                field.setMaintenanceFromDate(updateData.getMaintenanceFromDate());
            }

            if (updateData.getMaintenanceToDate() != null) {
                field.setMaintenanceToDate(updateData.getMaintenanceToDate());
            }

            return jpaFieldRepo.save(field);
        });
    }
}
