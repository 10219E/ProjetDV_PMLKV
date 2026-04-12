package lu.ephec.backend_projetdv2026.services;

import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.Field;
import lu.ephec.backend_projetdv2026.models.Match;
import lu.ephec.backend_projetdv2026.repo.JPAFieldRepo;
import lu.ephec.backend_projetdv2026.repo.JPAMatchRepo;
import lu.ephec.backend_projetdv2026.repo.JPASiteRepo;
import lu.ephec.backend_projetdv2026.services.validation.ValidationBoiler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class FieldService {

    private final JPAFieldRepo jpaFieldRepo;
    private final JPASiteRepo jpaSiteRepo;
    private final JPAMatchRepo jpaMatchRepo;


    // InjDep Interface Field
    public FieldService(JPAFieldRepo jpaFieldRepo, JPASiteRepo jpaSiteRepo, JPAMatchRepo jpaMatchRepo) {
        this.jpaFieldRepo = jpaFieldRepo;
        this.jpaSiteRepo = jpaSiteRepo;
        this.jpaMatchRepo = jpaMatchRepo;
    }

    //CHECK EXISTS
    public boolean fieldExists(Integer fieldId) {
        return jpaFieldRepo.existsById(fieldId);
    }

    // SET Field
    @Transactional
    public Field newField(Field field) {
        ValidationBoiler.verifyNotNull(field, "Field");
        ValidationBoiler.verifyNotNull(field.getSite(), "Site");
        ValidationBoiler.verifyNotNull(field.getSite().getSiteId(), "Site ID");
        ValidationBoiler.verifyDatesValid(field.getMaintenanceFromDate(), field.getMaintenanceToDate(), "Maintenance From / To Date");

        return jpaFieldRepo.save(field);
    }

    // GET Field by ID
    public Optional<Field> fetchById(Integer fieldId) {
        ValidationBoiler.verifyNotNull(fieldId, "Field ID");
        ValidationBoiler.verifyExists(jpaFieldRepo.existsById(fieldId), "Field", fieldId);
        return jpaFieldRepo.findById(fieldId);
    }

    // GET Fields by Site
    public List<Field> fetchBySite(Integer siteId) {
        ValidationBoiler.verifyNotNull(siteId, "Site ID");
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        return jpaFieldRepo.findBySite_SiteId(siteId);
    }

    // GET Active Fields By Active Site
    public List<Field> fetchActiveFieldsByActiveSite(Integer siteId) {
        ValidationBoiler.verifyNotNull(siteId, "Site ID");
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        jpaSiteRepo.findById(siteId).ifPresent(site -> {
            if (!site.getIsActive()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Site is inactive: " + siteId);
            }
        });
        return jpaFieldRepo.findBySite_SiteIdAndIsActiveTrue(siteId);
    }

    // GET Inactive Fields By Site
    public List<Field> fetchInactiveFieldsBySite(Integer siteId) {
        ValidationBoiler.verifyNotNull(siteId, "Site ID");
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        return jpaFieldRepo.findBySite_SiteIdAndIsActiveFalse(siteId);
    }

    // GET Fields In Maintenance for SiteID
    public List<Field> fetchFieldsInMaintenance(Integer siteId) {
        ValidationBoiler.verifyNotNull(siteId, "Site ID");
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        return jpaFieldRepo.findBySite_SiteIdAndMaintenanceFromDateIsNotNullAndMaintenanceToDateIsNotNull(siteId);
    }

    // GET ALL Fields
    public List<Field> fetchAll() {
        return jpaFieldRepo.findAll();
    }

    //RETURN TOTAL NUM OF FIELDS
    public Integer countFields() {
        return Math.toIntExact(jpaFieldRepo.count());
    }

    //GET ALL indoor Fields By Site
    public List<Field> fetchIndoorBySite(Integer siteId) {
        ValidationBoiler.verifyNotNull(siteId, "Site ID");
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        return jpaFieldRepo.findBySite_SiteIdAndIsIndoorTrue(siteId);
    }

    //GET ALL outdoor Fields By Site
    public List<Field> fetchOutdoorBySite(Integer siteId) {
        ValidationBoiler.verifyNotNull(siteId, "Site ID");
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);
        return jpaFieldRepo.findBySite_SiteIdAndIsIndoorFalse(siteId);
    }

    // DELETE Field --  -- ONLY SUPER ADMIN
    @Transactional
    public void deleteField(Integer fieldId) {
        ValidationBoiler.verifyNotNull(fieldId, "Field ID");
        ValidationBoiler.verifyExists(jpaFieldRepo.existsById(fieldId), "Field", fieldId);

        // SET ALL MATCHES ON THIS FIELD AS "Cancelled"
        List<Match> matchesOnField = jpaMatchRepo.findByField_FieldId(fieldId);
        matchesOnField.forEach(match -> {
            if (match.getType().equals("public")) {
                match.setPubStatus("cancelled");
            } else if (match.getType().equals("private")) {
                match.setPrivStatus("cancelled");
            }
        });
        jpaMatchRepo.saveAll(matchesOnField);

        jpaFieldRepo.deleteById(fieldId);
    }

    // UPDATE Field
    @Transactional
    public Optional<Field> updateField(Integer fieldId, Field updateData) {
        ValidationBoiler.verifyExists(jpaFieldRepo.existsById(fieldId), "Field", fieldId);
        AtomicBoolean fieldDeactivated = new AtomicBoolean(false);


        return jpaFieldRepo.findById(fieldId).map(field -> {
            if (updateData.getIsIndoor() != null) {
                field.setIsIndoor(updateData.getIsIndoor());
            }

            if (updateData.getIsActive() != null && !updateData.getIsActive() && field.getIsActive()) {
                fieldDeactivated.set(true);
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

            if  (updateData.getMaintenanceFromDate() != null && updateData.getMaintenanceToDate() != null) {
                ValidationBoiler.verifyDatesValid(field.getMaintenanceFromDate(), field.getMaintenanceToDate(), "Maintenance From / To Date");
            }

            //IF FIELD IS SET TO INACTIVE
            if (fieldDeactivated.get()) {

                // SET ALL MATCHES ON THIS FIELD AS "Cancelled"
                List<Match> matchesOnField = jpaMatchRepo.findByField_FieldId(fieldId);
                matchesOnField.forEach(match -> {
                    if (match.getType().equals("public")) {
                        match.setPubStatus("cancelled");
                    } else if (match.getType().equals("private")) {
                        match.setPrivStatus("cancelled");
                    }
                });
                jpaMatchRepo.saveAll(matchesOnField);
            }

            return jpaFieldRepo.save(field);
        });
    }

}
