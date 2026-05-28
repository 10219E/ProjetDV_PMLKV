package lu.ephec.backend_projetdv2026.services;
import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.*;
import lu.ephec.backend_projetdv2026.repo.*;
import lu.ephec.backend_projetdv2026.services.validation.MatriculeHandler;
import lu.ephec.backend_projetdv2026.services.validation.ValidationBoiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service //BEAN
public class UserService {
    private final JPAUserRepo jpaUserRepo;
    private final JPAUserPenaltiesRepo jpaUserPenaltiesRepo;
    private final MigrateUserDESTRUCTIVE migrateUser;
    private final PaymentService paymentService;
    private final JPAUserAccountsRepo jpaUserAccountsRepo;
    private final JPAMatchPaymentsRepo jpaMatchPaymentsRepo;
    private final JPAUserSiteRepo jpaUserSiteRepo;
    private final JPASiteRepo jpaSiteRepo;
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    // InjDep Interface User + Penalties
    public UserService(JPAUserRepo jpaUserRepo, JPAUserPenaltiesRepo jpaUserPenaltiesRepo, MigrateUserDESTRUCTIVE migrateUser, PaymentService paymentService, JPAUserAccountsRepo jpaUserAccountsRepo, JPAMatchPaymentsRepo jpaMatchPaymentsRepo, JPAUserSiteRepo jpaUserSiteRepo, JPASiteRepo jpaSiteRepo) {
        this.jpaUserRepo = jpaUserRepo;
        this.jpaUserPenaltiesRepo = jpaUserPenaltiesRepo;
        this.migrateUser = migrateUser;
        this.paymentService = paymentService;
        this.jpaUserAccountsRepo = jpaUserAccountsRepo;
        this.jpaMatchPaymentsRepo = jpaMatchPaymentsRepo;
        this.jpaUserSiteRepo = jpaUserSiteRepo;
        this.jpaSiteRepo = jpaSiteRepo;
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

    //TOUCH LAST LOGIN BY USERID
    @Transactional
    public void touchLastLoginByMatricule(String user_id) {
        logger.info("[Service - User] Touching last login for user: {}", user_id);
        jpaUserRepo.findById(user_id).ifPresent(user -> {
            user.setLastLogin(LocalDateTime.now());
            jpaUserRepo.save(user);
        });
    }

    //SET User -- with email verification (is unique)
    @Transactional //Makes sure the whole method is executed
    public User newUser(User user) {

        logger.info("[Service - User] Creating new user: {}", user.getMatricule());
        //GENERATE MATRICULE
        user.setMatricule(MatriculeHandler.generateMatricule(user.getRole().getId(), jpaUserRepo));

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
        logger.info("[Service - User] Saving user: {}", user.getMatricule());
        User savedUser = jpaUserRepo.save(user);

        //CREATE FINANCE ACCOUNT
        EnumUserRolesType roleType = EnumUserRolesType.fromId(savedUser.getRole().getId());
        if (roleType != null && !roleType.isAdmin()) { //only normal users should have financial accounts
            logger.info("[Service - User] Creating new user wallet: {}", savedUser.getMatricule());
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

    //GET All Users -- FOR SUPER ADMINS
    public List<User> fetchAll() { return jpaUserRepo.findAll(); }

    // FETCH ALL USER FOR SITE
    public List<User> fetchBySite(Integer siteId) {
        ValidationBoiler.verifyNotNull(siteId, "Site ID");
        ValidationBoiler.verifyExists(jpaSiteRepo.existsById(siteId), "Site", siteId);

        List<UsersSites> links = jpaUserSiteRepo.findBySite_SiteId(siteId);
        List<User> users = new java.util.ArrayList<>();
        for (UsersSites link : links) {
            users.add(link.getUser());
        }
        return users;
    }

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
        logger.warn("[Service - User] !!!Deleting user: {}", userId);
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);

        //DELETE PENALTIES
        try {
            logger.warn("[Service - User] !!!Deleting user penalties: {}", userId);
            jpaUserPenaltiesRepo.deleteAllByUserMatricule(userId);
        } catch (Exception ignored) { }

        //DELETE ACCOUNT
        try {
            logger.warn("[Service - User] !!!Deleting user wallet: {}", userId);
            jpaUserAccountsRepo.deleteByUser_Matricule(userId);
        } catch (Exception ignored) { }

        //DELETE MATCH PAYMENTS
        try {
            logger.warn("[Service - User] !!!Deleting user match payments: {}", userId);
            jpaMatchPaymentsRepo.deleteAll(jpaMatchPaymentsRepo.findByUser_Matricule(userId));
        } catch (Exception ignored) { }

        //DELETE USER SITE SUBS
        try {
            logger.warn("[Service - User] !!!Deleting user site subscriptions: {}", userId);
            jpaUserSiteRepo.deleteAll(jpaUserSiteRepo.findByUser_Matricule(userId));
        } catch (Exception ignored) { }

        //DELETE USER
        jpaUserRepo.deleteById(userId); //No interfacing needed - handled by JPARepo
    }

    //UPDATE User
    @Transactional //Makes sure the whole method is executed
    public Optional<User> updateUser(String userId, User updatedUser) {
        logger.info("[Service - User] Updating user: {}", userId);
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyNotNull(updatedUser, "Updated User");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        return jpaUserRepo.findById(userId).map(user -> {
            if (updatedUser.getIsActive() != null) {
                // Block deactivation when user still has financial obligations
                if (!updatedUser.getIsActive()) {
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

            logger.info("[Service - User] Saving updated user: {}", userId);
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
        logger.info("[Service - User : Penalty] Creating new penalty for user: {}", penalty.getUser().getMatricule());
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

        logger.info("[Service - User : Penalty] Saving new penalty for user: {}", penalty.getUser().getMatricule());
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
        logger.info("[Service - User : Penalty] Updating penalty: {}", penaltyId);
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

            logger.info("[Service - User : Penalty] Saving updated penalty: {}", penaltyId);
            return jpaUserPenaltiesRepo.save(penalty);
        });
    }

    //DELETE UNIQUE Penalty -- Only for Test cleanup
    public void deletePenalty(Integer penaltyId) {
        logger.warn("[Service - User : Penalty] !!!Deleting penalty: {}", penaltyId);
        ValidationBoiler.verifyNotNull(penaltyId, "Penalty ID");
        ValidationBoiler.verifyExists(jpaUserPenaltiesRepo.existsById(penaltyId), "Penalty", penaltyId);
        jpaUserPenaltiesRepo.deleteById(penaltyId);
    }

    //DELETE ALL PENALTIES for User by userId (clear history) -- Admin only
    @Transactional //Makes sure the whole method is executed
    public void deleteAllPenaltiesForUser(String userId) {
        logger.warn("[Service - User : Penalty] !!!Deleting all penalties for user: {}", userId);
        ValidationBoiler.verifyNotEmpty(userId, "User ID");
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(userId), "User", userId);
        jpaUserPenaltiesRepo.deleteAllByUserMatricule(userId);
    }


}
