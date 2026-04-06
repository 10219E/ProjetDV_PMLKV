package lu.ephec.backend_projetdv2026.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.*;
import lu.ephec.backend_projetdv2026.repo.*;
import lu.ephec.backend_projetdv2026.services.validation.MatriculeHandler;
import lu.ephec.backend_projetdv2026.services.validation.ValidationBoiler;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

//This class allows the migration of users from one role to another by creating a new user with the same data but a new matricule and role,
//then copying penalties to the new user and finally deleting the old user.
// This is necessary to avoid conflicts in the database due to the change of primary key (matricule) and role association.
// The migration is destructive as it deletes the old user, so it should be used with caution and only when necessary
// (for example when changing from subscribed to site admin or vice versa).
//It performs checks such as not migrating to an already assigned role and also requires clear history of penalties, etc
//EDIT:
////We can not call newUser() as there will be circular error, so we need to inject directly in the repo and handle the logic in the service, which is more complex but allows us to bypass the circular dependency issue.
////This happened since we defined migrateUser in UserService (expected)

@Service
public class MigrateUserDESTRUCTIVE {

    private final JPAUserRepo jpaUserRepo;
    private final JPAUserPenaltiesRepo jpaUserPenaltiesRepo;
    private final MatriculeHandler matriculeHandler;
    private final JPAMatchRepo jpaMatchRepo;
    private final JPAMatchPlayersRepo jpaMatchPlayersRepo;
    private final JPAUserAccountsRepo jpaUserAccountsRepo;
    private final JPAMatchPaymentsRepo jpaMatchPaymentsRepo;


    @PersistenceContext
    private EntityManager em;

    public MigrateUserDESTRUCTIVE(JPAUserRepo jpaUserRepo, JPAUserPenaltiesRepo jpaUserPenaltiesRepo,
                                  MatriculeHandler matriculeHandler, JPAMatchRepo jpaMatchRepo, JPAMatchPlayersRepo jpaMatchPlayersRepo, JPAUserAccountsRepo jpaUserAccountsRepo, JPAMatchPaymentsRepo jpaMatchPaymentsRepo) {
        this.jpaUserRepo = jpaUserRepo;
        this.jpaUserPenaltiesRepo = jpaUserPenaltiesRepo;
        this.matriculeHandler = matriculeHandler;
        this.jpaMatchRepo = jpaMatchRepo;
        this.jpaMatchPlayersRepo = jpaMatchPlayersRepo;
        this.jpaUserAccountsRepo = jpaUserAccountsRepo;
        this.jpaMatchPaymentsRepo = jpaMatchPaymentsRepo;
    }

    @Transactional
    public User migrateUserRole(String oldMatricule, Short newRoleId) {

        // Verify old user exists
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(oldMatricule), "User", oldMatricule);

        // Fetch current user data
        User oldUser = jpaUserRepo.findById(oldMatricule)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        //ONLY ACTIVE USER CAN BE MIGRATED
        ValidationBoiler.verifyUserActive(oldUser.getIsActive(), oldMatricule);

        //CHECK ROLE IS VALID
        ValidationBoiler.verifyValidRoleId(oldUser.getRole().getId());
        ValidationBoiler.verifyValidRoleId(newRoleId);

        // CHECK IF MIGRATION IS AUTHORIZED (can't migrate admin to normal user and vice versa for security reasons)
        ValidationBoiler.verifyNotMigrationBetweenAdminNormal(oldUser.getRole().getId(), newRoleId);

        // CHECK IF USER HAS ACTIVE PENALTIES - IF YES, ABORT MIGRATION
        List<UserPenalties> activePenalties = jpaUserPenaltiesRepo.findAllActiveWithUser(oldMatricule);
        boolean hasActivePenalties = !activePenalties.isEmpty();
        ValidationBoiler.verifyNoActivePenalties(hasActivePenalties, oldMatricule);

        //CHECK IF USER HAS DEBT OR OUTSTANDING MATCH PAYMENTS
        ValidationBoiler.verifyNoOutstandingFinancialObligations(jpaUserAccountsRepo.hasDebt(oldMatricule), jpaMatchPaymentsRepo.findByUser_Matricule(oldMatricule), oldMatricule);

        // Check if role is different (to avoid unnecessary migration)
        if (oldUser.getRole().getId().equals(newRoleId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User already has this role");
        }

        // Verify new role exists
        UserRoles newRole = em.find(UserRoles.class, newRoleId);
        ValidationBoiler.verifyExists(newRole != null, "Role", newRoleId);

        // 1 - FETCH DATA
        List<UserPenalties> oldPenalties = jpaUserPenaltiesRepo.findByUserMatriculeWithUser(oldMatricule);
        List<MatchPlayers> oldMatchPlayers = jpaMatchPlayersRepo.findByUser_Matricule(oldMatricule);
        List<Match> organizedMatches = jpaMatchRepo.findByOrganiser_Matricule(oldMatricule);
        String oldEmail = oldUser.getEmail();


        // 2 - CREATE NEW USER with same data but new role
        User newUser = new User();
        newUser.setIsActive(oldUser.getIsActive());
        newUser.setFirstName(oldUser.getFirstName());
        newUser.setLastName(oldUser.getLastName());
        newUser.setEmail("temp-" + oldUser.getMatricule() + UUID.randomUUID() + "@temp.tmp"); //temp email to avoid conflicts
        newUser.setBirthDate(oldUser.getBirthDate());
        newUser.setLevel(oldUser.getLevel());
        newUser.setAuth(oldUser.getAuth());
        newUser.setCreated(oldUser.getCreated());
        newUser.setLastLogin(oldUser.getLastLogin());
        newUser.setRole(newRole);

        // GENERATE NEW MATRICULE WITH NEW ROLE
        String newMatricule = matriculeHandler.generateMatricule(newRoleId, jpaUserRepo);
        newUser.setMatricule(newMatricule);

        // 3 - SAVE NEW USER
        User savedNewUser = jpaUserRepo.save(newUser);
        em.flush(); //Ensure new user is saved and has an ID before creating penalties

        // Always use a managed reference for relationships
        User managedNewUser = em.getReference(User.class, savedNewUser.getMatricule());

        
        // 4 - UPDATE MATCH OCCURENCES TO NEW USER
        organizedMatches.forEach(match -> match.setOrganiser(managedNewUser));
        jpaMatchRepo.saveAll(organizedMatches);

        // 5 - UPDATE MATCH PLAYERS OCCURENCES TO NEW USER
        oldMatchPlayers.forEach(matchPlayer -> matchPlayer.setUser(managedNewUser));
        jpaMatchPlayersRepo.saveAll(oldMatchPlayers);

        // 6 - UPDATE PREVIOUS MATCH PAYMENTS TO NEW USER
        List<MatchPayments> oldPayments = jpaMatchPaymentsRepo.findByUser_Matricule(oldMatricule);
        oldPayments.forEach(oldPayment -> {
            oldPayment.setUser(managedNewUser);
        });
        jpaMatchPaymentsRepo.saveAll(oldPayments);

        // 7 - UPDATE USER ACCOUNT TO NEW USER (reassign existing row)
        jpaUserAccountsRepo.findByUser_Matricule(oldMatricule).ifPresent(account -> {
            account.setUser(managedNewUser);
            account.setLastUpdate(LocalDateTime.now());
            jpaUserAccountsRepo.save(account);
        });

        // 8 - DELETE OLD USER (frees up the email and matricule)
        jpaUserPenaltiesRepo.deleteAll(oldPenalties); //1
        jpaUserRepo.deleteById(oldMatricule); //2
        em.flush(); //Clear session

        // 9 - UPDATE NEW USER EMAIL
        managedNewUser.setEmail(oldEmail);
        jpaUserRepo.save(managedNewUser);
        em.flush(); //FLUSH TO ENSURE ALL CHANGES ARE COMMITTED

        // 10 - CREATE NEW PENALTY INSTANCES (completely new, not merged)
        oldPenalties.forEach(oldPenalty -> {
            UserPenalties newPenalty = new UserPenalties();
            newPenalty.setUser(managedNewUser);  // Link to new user
            newPenalty.setReason(oldPenalty.getReason());
            newPenalty.setStartDate(oldPenalty.getStartDate());
            newPenalty.setEndDate(oldPenalty.getEndDate());
            newPenalty.setIsActive(oldPenalty.getIsActive());
            newPenalty.setDescription(oldPenalty.getDescription());
            newPenalty.setMatchId(oldPenalty.getMatchId());

            // Save the NEW penalty
            jpaUserPenaltiesRepo.save(newPenalty);
        });

        em.flush(); //FLUSH TO ENSURE ALL CHANGES ARE COMMITTED


        return managedNewUser;
    }

}