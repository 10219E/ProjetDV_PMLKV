package lu.ephec.backend_projetdv2026.Repository;

import lu.ephec.backend_projetdv2026.Models.User;
import lu.ephec.backend_projetdv2026.Repository.Interfaces.UserIntRepo;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserRepo {
    private final UserIntRepo userIntRepo;

    // InjDep Interface User
    public UserRepo(UserIntRepo userIntRepo) {
        this.userIntRepo = userIntRepo;
    }

    //SET User
    public User newUser(User user) {
        return userIntRepo.save(user);
    }

    //GET User by Matricule
    public Optional<User> fetchById(String userId) {
        return userIntRepo.findById(userId);
    }

    //GET User by email
    public Optional<User> fetchByMail(String email) {
        return userIntRepo.findByEmail(email);
    }

    //DELETE User
    public void delUser(String userId) {
        userIntRepo.deleteById(userId); //No interfacing needed - handled by JPARepo
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
