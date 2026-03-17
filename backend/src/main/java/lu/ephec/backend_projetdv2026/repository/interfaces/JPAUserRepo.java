package lu.ephec.backend_projetdv2026.repository.interfaces;

import lu.ephec.backend_projetdv2026.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JPAUserRepo extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findById(String matricule);
    //Delete user handled by JPARepo



}