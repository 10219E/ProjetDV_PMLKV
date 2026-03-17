package lu.ephec.backend_projetdv2026.repository.interfaces;

import lu.ephec.backend_projetdv2026.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface JPAUserRepo extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findById(String matricule);
    Optional<User> findByFirstName(String firstName);
    Optional<User> findByLastName(String lastName);


    @Query(value = "SELECT TOP 1 user_id FROM dbo.Users ORDER BY NEWID()", nativeQuery = true)
    Optional<String> fetchRandomUserId(); //Used in tests to fetch random ID

    //Delete user handled by JPA in UserRepo



}