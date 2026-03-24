package lu.ephec.backend_projetdv2026.services.validation;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.models.UserPenalties;
import lu.ephec.backend_projetdv2026.models.UserRoles;
import lu.ephec.backend_projetdv2026.services.UserService;
import lu.ephec.backend_projetdv2026.repo.JPAUserPenaltiesRepo;
import lu.ephec.backend_projetdv2026.repo.JPAUserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

//This class allows the migration of users from one role to another by creating a new user with the same data but a new matricule and role,
//then copying penalties to the new user and finally deleting the old user.
// This is necessary to avoid conflicts in the database due to the change of primary key (matricule) and role association.
// The migration is destructive as it deletes the old user, so it should be used with caution and only when necessary
// (for example when changing from subscribed to site admin or vice versa).
//It performs checks such as not migrating to an already assigned role and also requires clear history of penalties, etc

@Service
public class MigrateUserDESTRUCTIVE {

    private final JPAUserRepo jpaUserRepo;
    private final JPAUserPenaltiesRepo jpaUserPenaltiesRepo;
    private final UserService userService;

    @PersistenceContext
    private EntityManager em;

    public MigrateUserDESTRUCTIVE(JPAUserRepo jpaUserRepo, JPAUserPenaltiesRepo jpaUserPenaltiesRepo,
                                  MatriculeHandler matriculeHandler, UserService userService) {
        this.jpaUserRepo = jpaUserRepo;
        this.jpaUserPenaltiesRepo = jpaUserPenaltiesRepo;
        this.userService = userService;
    }

    @Transactional
    public User migrateUserRole(String oldMatricule, Short newRoleId) {

        // Verify old user exists
        ValidationBoiler.verifyExists(jpaUserRepo.existsById(oldMatricule), "User", oldMatricule);

        // Fetch current user data
        User oldUser = jpaUserRepo.findById(oldMatricule)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // CHECK IF USER HAS ACTIVE PENALTIES - IF YES, ABORT MIGRATION
        boolean hasActivePenalties = userService.hasActivePenalty(oldMatricule);
        ValidationBoiler.verifyNoActivePenalties(hasActivePenalties, oldMatricule);

        // Check if role is different (to avoid unnecessary migration)
        if (oldUser.getRole().getId().equals(newRoleId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "User already has this role");
        }

        // Verify new role exists
        UserRoles newRole = em.find(UserRoles.class, newRoleId);
        ValidationBoiler.verifyExists(newRole != null, "Role", newRoleId);

        // 1 - FETCH PENALTIES BEFORE DELETING OLD USER
        List<UserPenalties> oldPenalties = jpaUserPenaltiesRepo.findByUserMatriculeWithUser(oldMatricule);

        // 2 - DELETE OLD USER (frees up the email and matricule)
        jpaUserPenaltiesRepo.deleteAll(oldPenalties); //1
        jpaUserRepo.deleteById(oldMatricule); //2
        //MAYBE MATCH + SITE DELETION LATER
        em.flush(); //Clear session

        // 3 - CREATE NEW USER with same data but new role
        User newUser = new User();
        newUser.setIsActive(oldUser.getIsActive());
        newUser.setFirstName(oldUser.getFirstName());
        newUser.setLastName(oldUser.getLastName());
        newUser.setEmail(oldUser.getEmail());
        newUser.setBirthDate(oldUser.getBirthDate());
        newUser.setLevel(oldUser.getLevel());
        newUser.setAuth(oldUser.getAuth());
        newUser.setCreated(oldUser.getCreated());
        newUser.setLastLogin(oldUser.getLastLogin());
        newUser.setRole(newRole);

        // 4 - SAVE NEW USER
        User savedNewUser = userService.newUser(newUser);
        em.flush(); //Ensure new user is saved and has an ID before creating penalties

        // 5 - CREATE NEW PENALTY INSTANCES (completely new, not merged)
        oldPenalties.forEach(oldPenalty -> {
            UserPenalties newPenalty = new UserPenalties();
            newPenalty.setUser(savedNewUser);  // Link to new user
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

        return savedNewUser;
    }

}