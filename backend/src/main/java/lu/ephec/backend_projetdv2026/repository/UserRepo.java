package lu.ephec.backend_projetdv2026.repository;

import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.repository.interfaces.JPAUserRepo;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service //BEAN
public class UserRepo {
    private final JPAUserRepo jpaUserRepo;

    // InjDep Interface User
    public UserRepo(JPAUserRepo jpaUserRepo) {
        this.jpaUserRepo = jpaUserRepo;
    }

    //SET User
    public User newUser(User user) {
        return jpaUserRepo.save(user);
    }

    //GET User by Matricule
    public Optional<User> fetchById(String userId) {
        return jpaUserRepo.findById(userId);
    }

    //GET User by email
    public Optional<User> fetchByMail(String email) {
        return jpaUserRepo.findByEmail(email);
    }

    //GET User by Name or FirstName --filtering on both as could be multiple users with same name or first name
    public List<User> fetchByName(String gname) {
        if (gname == null) {
            return Collections.emptyList();
        }
        return jpaUserRepo.findByFirstNameIgnoreCaseOrLastNameIgnoreCase(gname, gname);
    }

    //GET All Users
    public List<User> findAll() { return jpaUserRepo.findAll(); }

    //DELETE User
    public void delUser(String userId) {
        jpaUserRepo.deleteById(userId); //No interfacing needed - handled by JPARepo
    }

    //UPDATE User
    public Optional<User> updUser(String userId, User updatedUser) {
        return jpaUserRepo.findById(userId).map(user -> {
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

            return jpaUserRepo.save(user);
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
