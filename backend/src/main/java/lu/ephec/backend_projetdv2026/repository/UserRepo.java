package lu.ephec.backend_projetdv2026.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.models.UserPenalties;
import lu.ephec.backend_projetdv2026.models.UserRoles;
import lu.ephec.backend_projetdv2026.repository.interfaces.JPAUserPenaltiesRepo;
import lu.ephec.backend_projetdv2026.repository.interfaces.JPAUserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Service //BEAN
public class UserRepo {
    private final JPAUserRepo jpaUserRepo;
    private final JPAUserPenaltiesRepo jpaUserPenaltiesRepo;

    // InjDep Interface User + Penalties
    public UserRepo(JPAUserRepo jpaUserRepo, JPAUserPenaltiesRepo jpaUserPenaltiesRepo) {
        this.jpaUserRepo = jpaUserRepo;
        this.jpaUserPenaltiesRepo = jpaUserPenaltiesRepo;
    }

    ////////////USER OPERATIONS
    //CHECK EXISTS
    public boolean userExists(String userId) {
        return jpaUserRepo.existsById(userId);
    }

    //CHECK EMAIL Exists
    public boolean emailExists(String email) {
        return jpaUserRepo.existsByEmail(email);
    }

    //FETCH BY LEVEL
    public List<User> fetchByLevel(String level) {
        if (level == null || level.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return jpaUserRepo.findAllByLevelIgnoreCase(level);
    }

    //SET User -- with email verification (is unique)
    @Transactional //Makes sure the whole method is executed
    public User newUser(User user) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (jpaUserRepo.existsByEmail(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
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

    //GET Users by Role
    public List<User> fetchByRole(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return jpaUserRepo.findAllWithRoleByName(roleName);
    }

    //GET Active Users
    public List<User> findAllActive() {
        return jpaUserRepo.findAllByIsActiveTrue();
    }

    //GET Inactive Users
    public List<User> findAllInactive() {
        return jpaUserRepo.findAllByIsActiveFalse();
    }

    //DELETE User -- FOR SUPER ADMIN ONLY
    @Transactional //Makes sure the whole method is executed
    public void deleteUser(String userId) {
        jpaUserRepo.deleteById(userId); //No interfacing needed - handled by JPARepo
    }

    @PersistenceContext
    private EntityManager em; //Tool to read DB data for User Roles

    //UPDATE User
    @Transactional //Makes sure the whole method is executed
    public Optional<User> updateUser(String userId, User updatedUser) {
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

            if (updatedUser.getRole() != null && updatedUser.getRole().getId() != null) {
                Short newRoleId = updatedUser.getRole().getId(); //No direct method as referenced value - need to get ID from Role object
                UserRoles roleEntity = em.find(UserRoles.class, newRoleId);
                if (roleEntity == null) { //Checking User Role exists in DB before updating - otherwise would cause error on save
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Role: " + newRoleId);
                }
                user.setRole(roleEntity); //Updating if no throw
            }

            return jpaUserRepo.save(user);
        });
    }

    ////////////PENALTIES

    //HAS ACTIVE Penalty check
    public boolean hasActivePenalty(String userId) {
        return jpaUserPenaltiesRepo.existsActivePenaltyAt(userId, LocalDateTime.now());
    }

    //SET PENALTY to User
    @Transactional //Makes sure the whole method is executed
    public UserPenalties newPenalty(UserPenalties penalty) { return jpaUserPenaltiesRepo.save(penalty);
    }

    //GET PENALTY for User
    public UserPenalties fetchPenaltyByUser(String userId) {
        return jpaUserPenaltiesRepo.findByUserMatriculeWithUser(userId).orElse(null);
    }

    //COUNT PENALTY for User
    public long countPenaltiesForUser(String userId) {
        return jpaUserPenaltiesRepo.countByUserMatricule(userId);
    }

    //ALL ACTIVE Penalties
    public List<UserPenalties> findAllPenalties() { return jpaUserPenaltiesRepo.findAllWithUser(); }

    //LIST Penalties by reason
    public List<UserPenalties> listAllPenaltiesByReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return jpaUserPenaltiesRepo.findAllWithUserByReason(reason);
    }

    //UPDATE Penalty
    @Transactional //Makes sure the whole method is executed
    public Optional<UserPenalties> updatePenalty(Integer penaltyId, UserPenalties updatedPenalty) {
        return jpaUserPenaltiesRepo.findById(penaltyId).map(penalty -> {
            if (updatedPenalty.getReason() != null) {
                penalty.setReason(updatedPenalty.getReason());
            }

            if (updatedPenalty.getStartDate() != null) {
                penalty.setStartDate(updatedPenalty.getStartDate());
            }

            if (updatedPenalty.getEndDate() != null) {
                penalty.setEndDate(updatedPenalty.getEndDate());
            }

            // Validate dates after potential update
            if (penalty.getStartDate() != null && penalty.getEndDate() != null) {
                if (penalty.getStartDate().isAfter(penalty.getEndDate())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "start_date must be <= end_date");
                }
            }

            if (updatedPenalty.getIsActive() != null) {
                penalty.setIsActive(updatedPenalty.getIsActive());
            }

            if (updatedPenalty.getDescription() != null) {
                penalty.setDescription(updatedPenalty.getDescription());
            }

            return jpaUserPenaltiesRepo.save(penalty);
        });
    }

    //DELETE UNIQUE Penalty -- no single penalty delete
    //public void deletePenalty(Integer penaltyId) {
    //    jpaUserPenaltiesRepo.deleteById(penaltyId);
    //}

    //DELETE ALL PENALTIES for User by userId (clear history) -- Admin only
    @Transactional //Makes sure the whole method is executed
    public void deletePenalty(String userId) {
        jpaUserPenaltiesRepo.deleteAllByUserMatricule(userId);
    }


}
