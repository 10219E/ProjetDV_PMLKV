package lu.ephec.backend_projetdv2026.services;
import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.EnumUserRolesType;
import lu.ephec.backend_projetdv2026.models.MatchPayments;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.models.UserPenalties;
import lu.ephec.backend_projetdv2026.repo.*;
import lu.ephec.backend_projetdv2026.services.validation.MatriculeHandler;
import lu.ephec.backend_projetdv2026.services.validation.ValidationBoiler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service //BEAN
public class UserService {
    private final JPAUserRepo jpaUserRepo;
    private final JPAUserPenaltiesRepo jpaUserPenaltiesRepo;
    private final MatriculeHandler matriculeHandler;
    private final MigrateUserDESTRUCTIVE migrateUser;
    private final PaymentService paymentService;
    private final JPAUserAccountsRepo jpaUserAccountsRepo;
    private final JPAMatchPaymentsRepo jpaMatchPaymentsRepo;
    private final JPAUserSiteRepo jpaUserSiteRepo;

    // InjDep Interface User + Penalties
    public UserService(JPAUserRepo jpaUserRepo, JPAUserPenaltiesRepo jpaUserPenaltiesRepo, MatriculeHandler matriculeHandler, MigrateUserDESTRUCTIVE migrateUser, PaymentService paymentService, JPAUserAccountsRepo jpaUserAccountsRepo, JPAMatchPaymentsRepo jpaMatchPaymentsRepo, JPAUserSiteRepo jpaUserSiteRepo) {
        this.jpaUserRepo = jpaUserRepo;
        this.jpaUserPenaltiesRepo = jpaUserPenaltiesRepo;
        this.matriculeHandler = matriculeHandler;
        this.migrateUser = migrateUser;
        this.paymentService = paymentService;
        this.jpaUserAccountsRepo = jpaUserAccountsRepo;
        this.jpaMatchPaymentsRepo = jpaMatchPaymentsRepo;
        this.jpaUserSiteRepo = jpaUserSiteRepo;
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
        ValidationBoiler.verifyValidLevel(level);  // Validates both null/empty AND valid values
        return jpaUserRepo.findAllByLevelIgnoreCase(level);
    }

    @Transactional
    //MIGRATE User to new Role -- For Admin USE only
    public User migrateUser(String oldMatricule, Short newRoleId) {
        ValidationBoiler.verifyNotEmpty(oldMatricule, "User matricule");
        ValidationBoiler.verifyNotNull(newRoleId, "New role ID");

        return migrateUser.migrateUserRole(oldMatricule, newRoleId);
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

        //Validate Role
        ValidationBoiler.verifyValidRoleId(user.getRole().getId());

        //For admin roles (M=7, A=9), set level to null; for normal users, validate level
        if (user.getLevel() != null) {
            //CHECK NOT admin --admin level should be null
            ValidationBoiler.verifyNotAdminUser(user.getRole().getId(), user.getMatricule());

            //VALIDATE Level for normal users
            ValidationBoiler.verifyValidLevel(user.getLevel());
            user.setLevel(user.getLevel());
        }

        //SAVE USER
        User savedUser = jpaUserRepo.save(user);

        //CREATE FINANCE ACCOUNT
        EnumUserRolesType roleType = EnumUserRolesType.fromId(savedUser.getRole().getId());
        if (roleType != null && !roleType.isAdmin()) { //only normal users should have financial accounts
            paymentService.newUserAccount(savedUser.getMatricule());
        }

        return savedUser;
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
        ValidationBoiler.verifyNotEmpty(gname, "Given Name");
        return jpaUserRepo.findByFirstNameIgnoreCaseOrLastNameIgnoreCase(gname, gname);
    }

    //GET All Users
    public List<User> fetchAll() { return jpaUserRepo.findAll(); }

    //GET Users by ROLE ID
    public List<User> fetchByRoleId(Short roleId) {
        ValidationBoiler.verifyNotNull(roleId, "Role ID");

        // Validate role exists
        ValidationBoiler.verifyValidRoleId(roleId);

        return jpaUserRepo.findAllByRoleId(roleId);
    }

    //GET Active Users
    public List<User> fetchAllActiveUsers() {
        return jpaUserRepo.findAllByIsActiveTrue();
    }

    //GET Inactive Users
    public List<User> fetchAllInactiveUsers() {
        return jpaUserRepo.findAllByIsActiveFalse();
    }

    //DELETE User -- FOR SUPER ADMIN ONLY AND GENERALLY SHOULD NOT BE USED
    @Transactional //Makes sure the whole method is executed
    public void deleteUser(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);

        //DELETE PENALTIES
        try {
            jpaUserPenaltiesRepo.deleteAllByUserMatricule(userId);
        } catch (Exception ignored) { }

        //DELETE ACCOUNT
        try {
            jpaUserAccountsRepo.deleteByUser_Matricule(userId);
        } catch (Exception ignored) { }

        //DELETE MATCH PAYMENTS
        try {
            jpaMatchPaymentsRepo.deleteAll(jpaMatchPaymentsRepo.findByUser_Matricule(userId));
        } catch (Exception ignored) { }

        //DELETE USER SITE SUBS
        try {
            jpaUserSiteRepo.deleteAll(jpaUserSiteRepo.findByUser_Matricule(userId));
        } catch (Exception ignored) { }

        //DELETE USER
        jpaUserRepo.deleteById(userId); //No interfacing needed - handled by JPARepo
    }

    //UPDATE User
    @Transactional //Makes sure the whole method is executed
    public Optional<User> updateUser(String userId, User updatedUser) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyNotNull(updatedUser, "Updated User");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        return jpaUserRepo.findById(userId).map(user -> {
            if (updatedUser.getIsActive() != null) {
                // Block deactivation when user still has financial obligations
                if (Boolean.FALSE.equals(updatedUser.getIsActive())) {
                    boolean hasDebt = paymentService.userHasDebt(userId);
                    List<MatchPayments> pendingPayments = jpaMatchPaymentsRepo.findByUser_MatriculeAndStatus(userId, "pending");
                    ValidationBoiler.verifyNoOutstandingFinancialObligations(hasDebt, pendingPayments, userId);
                }
                user.setIsActive(updatedUser.getIsActive());
            }

            if (updatedUser.getFirstName() != null) {
                ValidationBoiler.verifyNotEmpty(updatedUser.getFirstName(), "First name");
                user.setFirstName(updatedUser.getFirstName());
            }
            if (updatedUser.getEmail() != null) {
                //CHECK IF MAIL EXISTS
                if (!updatedUser.getEmail().equals(user.getEmail())) {
                    ValidationBoiler.verifyNotEmpty(updatedUser.getEmail(), "Email");
                    ValidationBoiler.verifyEmailNotExists(jpaUserRepo.existsByEmail(updatedUser.getEmail()), updatedUser.getEmail());
                }
                user.setEmail(updatedUser.getEmail());
            }

            if (updatedUser .getLastName() != null) {
                ValidationBoiler.verifyNotEmpty(updatedUser.getLastName(), "Last name");
                user.setLastName(updatedUser.getLastName());
            }

            if (updatedUser.getBirthDate() != null) {
                user.setBirthDate(updatedUser.getBirthDate());
            }

            if (updatedUser.getLevel() != null) {
                //CHECK IF ROLE IS VALID
                ValidationBoiler.verifyValidRoleId(user.getRole().getId());
                //CHECK NOT admin --admin level should be null
                ValidationBoiler.verifyNotAdminUser(user.getRole().getId(), user.getMatricule());

                //VALIDATE Level for normal users
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
        ValidationBoiler.verifyNotNull(penalty.getUser(), "User");
        ValidationBoiler.verifyNotNull(penalty.getUser().getMatricule(), "User matricule");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(penalty.getUser().getMatricule()),
                "User", penalty.getUser().getMatricule());
        ValidationBoiler.verifyUserActive(penalty.getUser().getIsActive(), penalty.getUser().getMatricule());

        //CHECK IF USER IS ADMIN - BLOCK IF YES
        User penalizedUser = jpaUserRepo.findById(penalty.getUser().getMatricule()).orElseThrow();
        ValidationBoiler.verifyNotAdminUser(penalizedUser.getRole().getId(), penalty.getUser().getMatricule());

        //REASON IS MANDATORY
        ValidationBoiler.verifyNotEmpty(penalty.getReason(), "Penalty reason");
        ValidationBoiler.verifyValidPenaltyReason(penalty.getReason());

        //DATES ARE MANDATORY AND VALID
        ValidationBoiler.verifyNotNull(penalty.getStartDate(), "Penalty start date");
        ValidationBoiler.verifyNotNull(penalty.getEndDate(), "Penalty end date");
        ValidationBoiler.verifyDatesValid(penalty.getStartDate(), penalty.getEndDate(), "Penalty dates");

        return jpaUserPenaltiesRepo.save(penalty);
    }

    //GET PENALTIES for User
    public List<UserPenalties> fetchPenaltyByUser(String userId) {
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        return jpaUserPenaltiesRepo.findByUserMatriculeWithUser(userId);
    }

    //GET ACTIVE PENALTIES for User
    public List<UserPenalties> fetchActivePenaltyByUser(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        return jpaUserPenaltiesRepo.findAllActiveWithUser(userId);
    }


    //COUNT PENALTY for User
    public long countPenaltiesForUser(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        return jpaUserPenaltiesRepo.countByUserMatricule(userId);
    }

    //GET ALL Penalties
    public List<UserPenalties> fetchAllPenalties() { return jpaUserPenaltiesRepo.findAll(); }

    //GET ALL ACTIVE Penalties
    public List<UserPenalties> fetchAllActivePenalties() { return jpaUserPenaltiesRepo.findAllByIsActiveTrue(); }

    //PENALTIES FOR USER BY Date Range
    public List<UserPenalties> fetchPenaltiesByUserRange(String userId, LocalDateTime startDate, LocalDateTime endDate) {
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        ValidationBoiler.verifyDatesValid(startDate, endDate, "Date range");
        return jpaUserPenaltiesRepo.fetchPenaltiesByUserAndDateRange(userId, startDate, endDate);
    }

    //LIST Penalties by reason
    public List<UserPenalties> listAllPenaltiesByReason(String reason) {
        ValidationBoiler.verifyNotEmpty(reason, "Reason");
        ValidationBoiler.verifyValidPenaltyReason(reason);
        return jpaUserPenaltiesRepo.findAllWithUserByReason(reason);
    }

    //UPDATE Penalty
    @Transactional //Makes sure the whole method is executed
    public Optional<UserPenalties> updatePenalty(Integer penaltyId, UserPenalties updatedPenalty) {
        ValidationBoiler.verifyNotNull(penaltyId, "Penalty ID");
        ValidationBoiler.verifyNotNull(updatedPenalty, "Update data");
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
        ValidationBoiler.verifyNotNull(penaltyId, "Penalty ID");
        ValidationBoiler.verifyExists(jpaUserPenaltiesRepo.existsById(penaltyId), "Penalty", penaltyId);
        jpaUserPenaltiesRepo.deleteById(penaltyId);
    }

    //DELETE ALL PENALTIES for User by userId (clear history) -- Admin only
    @Transactional //Makes sure the whole method is executed
    public void deleteAllPenaltiesForUser(String userId) {
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        jpaUserPenaltiesRepo.deleteAllByUserMatricule(userId);
    }


}
