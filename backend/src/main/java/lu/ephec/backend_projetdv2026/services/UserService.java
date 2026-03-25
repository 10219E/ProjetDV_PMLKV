package lu.ephec.backend_projetdv2026.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.models.UserPenalties;
import lu.ephec.backend_projetdv2026.repo.JPAUserPenaltiesRepo;
import lu.ephec.backend_projetdv2026.repo.JPAUserRepo;
import lu.ephec.backend_projetdv2026.services.validation.MatriculeHandler;
import lu.ephec.backend_projetdv2026.services.validation.ValidationBoiler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Service //BEAN
public class UserService {
    private final JPAUserRepo jpaUserRepo;
    private final JPAUserPenaltiesRepo jpaUserPenaltiesRepo;
    private final MatriculeHandler matriculeHandler;

    // InjDep Interface User + Penalties
    public UserService(JPAUserRepo jpaUserRepo, JPAUserPenaltiesRepo jpaUserPenaltiesRepo, MatriculeHandler matriculeHandler) {
        this.jpaUserRepo = jpaUserRepo;
        this.jpaUserPenaltiesRepo = jpaUserPenaltiesRepo;
        this.matriculeHandler = matriculeHandler;
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

        //GENERATE MATRICULE
        user.setMatricule(matriculeHandler.generateMatricule(user.getRole().getId(), jpaUserRepo));

        //VALIDATE
        ValidationBoiler.verifyNotExists(jpaUserRepo.existsById(user.getMatricule()), "User", user.getMatricule());
        ValidationBoiler.verifyNotEmpty(user.getEmail(), "Email");
        ValidationBoiler.verifyEmailNotExists(jpaUserRepo.existsByEmail(user.getEmail()), user.getEmail());

        //For admin roles (M=7, A=9), set level to null; for normal users, validate level
        Short roleId = user.getRole().getId();
        if (roleId == 7 || roleId == 9) {
            // Admin role - no level needed
            user.setLevel(null);
        } else {
            // Normal user role - level is required
            ValidationBoiler.verifyValidLevel(user.getLevel());
        }

        return jpaUserRepo.save(user);
    }

    //GET User by Matricule
    public Optional<User> fetchById(String userId) {
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        return jpaUserRepo.findById(userId);
    }

    //GET User by email
    public Optional<User> fetchByMail(String email) {
        ValidationBoiler.verifyExists(jpaUserRepo.existsByEmail(email), "User", email);
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
    public List<User> fetchAll() { return jpaUserRepo.findAll(); }

    //GET Users by Role
    public List<User> fetchByRole(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return jpaUserRepo.findAllWithRoleByName(roleName);
    }

    //GET Active Users
    public List<User> fetchAllActive() {
        return jpaUserRepo.findAllByIsActiveTrue();
    }

    //GET Inactive Users
    public List<User> fetchAllInactive() {
        return jpaUserRepo.findAllByIsActiveFalse();
    }

    //DELETE User -- FOR SUPER ADMIN ONLY
    @Transactional //Makes sure the whole method is executed
    public void deleteUser(String userId) {
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        jpaUserRepo.deleteById(userId); //No interfacing needed - handled by JPARepo
    }

    @PersistenceContext
    private EntityManager em; //Tool to read DB data for User Roles

    //UPDATE User
    @Transactional //Makes sure the whole method is executed
    public Optional<User> updateUser(String userId, User updatedUser) {
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        return jpaUserRepo.findById(userId).map(user -> {
            if (updatedUser.getIsActive() != null) {
                user.setIsActive(updatedUser.getIsActive());
            }

            if (updatedUser.getFirstName() != null) {
                user.setFirstName(updatedUser.getFirstName());
            }
            if (updatedUser.getEmail() != null) {
                //CHECK IF MAIL EXISTS
                if (!updatedUser.getEmail().equals(user.getEmail())) {
                    ValidationBoiler.verifyEmailNotExists(jpaUserRepo.existsByEmail(updatedUser.getEmail()), updatedUser.getEmail());
                }
                user.setEmail(updatedUser.getEmail());
            }

            if (updatedUser .getLastName() != null) {
                user.setLastName(updatedUser.getLastName());
            }

            if (updatedUser.getBirthDate() != null) {
                user.setBirthDate(updatedUser.getBirthDate());
            }

            if (updatedUser.getLevel() != null) {
                //Check if user is admin - admins can't have a level
                Short userRoleId = user.getRole().getId();
                if (userRoleId == 7 || userRoleId == 9) {
                    // Admin user - level must stay null
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Admin users cannot have a skill level. Level must remain null.");
                }
                //Normal user - validate level
                ValidationBoiler.verifyValidLevel(updatedUser.getLevel());
                user.setLevel(updatedUser.getLevel());
            }

            if (updatedUser.getAuth() != null) {
                user.setAuth(updatedUser.getAuth());
            }

            return jpaUserRepo.save(user);
        });
    }

    ////////////PENALTIES

    //HAS ACTIVE Penalty check
    public boolean hasActivePenalty(String userId) {
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        return jpaUserPenaltiesRepo.existsActivePenaltyAt(userId, LocalDateTime.now());
    }

    //SET PENALTY to User
    @Transactional //Makes sure the whole method is executed
    public UserPenalties newPenalty(UserPenalties penalty) {
        //CHECK USR
        if (penalty.getUser() == null || penalty.getUser().getMatricule() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is required for penalty");
        }
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(penalty.getUser().getMatricule()),
                "User", penalty.getUser().getMatricule());

        //REASON IS MANDATORY
        ValidationBoiler.verifyNotEmpty(penalty.getReason(), "Penalty reason");
        ValidationBoiler.verifyValidPenaltyReason(penalty.getReason());

        //DATES ARE MANDATORY AND VALID
        if (penalty.getStartDate() == null || penalty.getEndDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date and end date are required");
        }
        ValidationBoiler.verifyDatesValid(penalty.getStartDate(), penalty.getEndDate(), "Penalty dates");

        return jpaUserPenaltiesRepo.save(penalty);
    }

    //GET PENALTIES for User
    public List<UserPenalties> fetchPenaltyByUser(String userId) {
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        return jpaUserPenaltiesRepo.findByUserMatriculeWithUser(userId);
    }

    //COUNT PENALTY for User
    public long countPenaltiesForUser(String userId) {
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        return jpaUserPenaltiesRepo.countByUserMatricule(userId);
    }

    //ALL ACTIVE Penalties
    public List<UserPenalties> fetchAllPenalties() { return jpaUserPenaltiesRepo.findAllWithUser(); }

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
        ValidationBoiler.verifyExists(jpaUserPenaltiesRepo.existsById(penaltyId), "Penalty", penaltyId);
        return jpaUserPenaltiesRepo.findById(penaltyId).map(penalty -> {
            if (updatedPenalty.getReason() != null) {
                ValidationBoiler.verifyValidPenaltyReason(updatedPenalty.getReason());
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
                ValidationBoiler.verifyDatesValid(penalty.getStartDate(), penalty.getEndDate(), "Penalty dates");
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

    //DELETE UNIQUE Penalty -- Only for Test cleanup
    public void deletePenalty(Integer penaltyId) {
        ValidationBoiler.verifyExists(jpaUserPenaltiesRepo.existsById(penaltyId), "Penalty", penaltyId);
        jpaUserPenaltiesRepo.deleteById(penaltyId);
    }

    //DELETE ALL PENALTIES for User by userId (clear history) -- Admin only
    @Transactional //Makes sure the whole method is executed
    public void deleteAllPenaltiesForUser(String userId) {
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        jpaUserPenaltiesRepo.deleteAllByUserMatricule(userId);
    }


}
