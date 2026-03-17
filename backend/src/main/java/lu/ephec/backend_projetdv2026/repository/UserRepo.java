package lu.ephec.backend_projetdv2026.repository;

import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.repository.interfaces.JPAUserRepo;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserRepo {
    private final JPAUserRepo JPAUserRepo;

    // InjDep Interface User
    public UserRepo(JPAUserRepo JPAUserRepo) {
        this.JPAUserRepo = JPAUserRepo;
    }

    //SET User
    public User newUser(User user) {
        return JPAUserRepo.save(user);
    }

    //GET User by Matricule
    public Optional<User> fetchById(String userId) {
        return JPAUserRepo.findById(userId);
    }

    //GET User by email
    public Optional<User> fetchByMail(String email) {
        return JPAUserRepo.findByEmail(email);
    }

    //GET User by Name or FirstName
    public Optional<User> fetchByName(String name) {
        return JPAUserRepo.findByFirstName(name).or(() -> JPAUserRepo.findByLastName(name));
    }

    //DELETE User
    public void delUser(String userId) {
        JPAUserRepo.deleteById(userId); //No interfacing needed - handled by JPARepo
    }

    //UPDATE User
    public Optional<User> updUser(String userId, User updatedUser) {
        return JPAUserRepo.findById(userId).map(user -> {
            if (updatedUser.getIsActive() != null) {
                user.setIsActive(updatedUser.getIsActive());
            }

            if (updatedUser.getFirstName() != null) {
                user.setFirstName(updatedUser.getFirstName());
            }
            if (updatedUser.getEmail() != null) {
                user.setEmail(updatedUser.getEmail());
            }

            if (updatedUser .getLastName() != null) {
                user.setLastName(updatedUser.getLastName());
            }

            if (updatedUser.getBirthDate() != null) {
                user.setBirthDate(updatedUser.getBirthDate());
            }

            if (updatedUser.getLevel() != null) {
                user.setLevel(updatedUser.getLevel());
            }

            if (updatedUser.getAuth() != null) {
                user.setAuth(updatedUser.getAuth());
            }

            if (updatedUser.getRoleId() != null) {
                user.setRoleId(updatedUser.getRoleId());
            }

            return JPAUserRepo.save(user);
        });
    }

    /*
    //PENALTY
    public Optional<User> addPenalty(Long userId, Penalty penalty) {
        return userIntRepo.findById(userId).map(user -> {
            penalty.setUser(user);  // Set the relationship
            user.getPenalties().add(penalty);
            return userIntRepo.save(user);
        });
    }*/
}
