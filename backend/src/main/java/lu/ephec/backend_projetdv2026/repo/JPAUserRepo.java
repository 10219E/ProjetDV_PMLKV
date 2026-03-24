package lu.ephec.backend_projetdv2026.repo;

import lu.ephec.backend_projetdv2026.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JPAUserRepo extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    //Optional<User> findById(String matricule); //Handled by JPA with findById() method
    List<User> findAllByIsActiveTrue();
    List<User> findAllByIsActiveFalse();
    List<User> findAllByLevelIgnoreCase(String level);
    boolean existsByEmail(String email);

    //LIST to handle multiple users with same name or first name
    List<User> findByFirstNameIgnoreCaseOrLastNameIgnoreCase(String firstName, String lastName); //JPA Will detect the OR in the property name (+CI)
    //Optional<User> findByFirstName(String firstName);
    //Optional<User> findByLastName(String lastName);

    @Query("SELECT u FROM User u JOIN FETCH u.role WHERE LOWER(u.role.name) = LOWER(:roleName)")
    List<User> findAllWithRoleByName(@Param("roleName") String roleName);

    //FOR MATRICULE HANDLING TO IDENTIFY BY PREFIX (for example L0002, G0001 where prefix would be L or G)
    @Query("SELECT u FROM User u WHERE u.matricule LIKE :prefix%")
    List<User> findAllWithMatriculePrefix(@Param("prefix") String prefix);


    //@Query(value = "SELECT TOP 1 user_id FROM dbo.Users ORDER BY NEWID()", nativeQuery = true)
    //Optional<String> fetchRandomUserId(); //Wanted to use in tests to fetch random ID -- Done differently in Test as not needed in Prod

    //Delete user handled by JPA in UserService
}

