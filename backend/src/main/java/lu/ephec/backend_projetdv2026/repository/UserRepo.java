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

    //DELETE User
    public void delUser(String userId) {
        JPAUserRepo.deleteById(userId); //No interfacing needed - handled by JPARepo
    }

    /*
    //UPDATE User
    public Optional<User> updUser(Long userId, User updatedUser) {
        return userIntRepo.findById(userId).map(user -> {
            if (updatedUser.getName() != null) {
                user.setName(updatedUser.getName());
            }
            if (updatedUser.getEmail() != null) {
                user.setEmail(updatedUser.getEmail());
            }
            return userIntRepo.save(user);
        });
    }

    //PENALTY
    public Optional<User> addPenalty(Long userId, Penalty penalty) {
        return userIntRepo.findById(userId).map(user -> {
            penalty.setUser(user);  // Set the relationship
            user.getPenalties().add(penalty);
            return userIntRepo.save(user);
        });
    }*/
}
