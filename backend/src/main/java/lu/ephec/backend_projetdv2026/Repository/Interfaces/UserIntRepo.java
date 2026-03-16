package lu.ephec.backend_projetdv2026.Repository.Interfaces;

import lu.ephec.backend_projetdv2026.Models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserIntRepo extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findById(String matricule);
    //Delete user handled by JPARepo



}