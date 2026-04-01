package lu.ephec.backend_projetdv2026.repo;

import lu.ephec.backend_projetdv2026.models.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JPAMatchRepo extends JpaRepository<Match, Integer> {


    List<Match> findByType(String type);

    List<Match> findByTypeAndPubStatus(String type, String status);


    List<Match> findByTypeAndPrivStatus(String type, String status);

    List<Match> findByMatchDateBetween(LocalDate startDate, LocalDate endDate);

    List<Match> findByOrganiser_Matricule(String organiserId);

    @Query("SELECT m FROM Match m WHERE m.field.site.siteId = :siteId")
    List<Match> findBySiteId(@Param("siteId") Integer siteId);

    List<Match> findByField_FieldId(Integer fieldId);
}
