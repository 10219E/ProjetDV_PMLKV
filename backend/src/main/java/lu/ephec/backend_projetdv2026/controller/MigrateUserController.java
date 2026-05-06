package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.services.MigrateUserDESTRUCTIVE;
import lu.ephec.backend_projetdv2026.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/migration")
public class MigrateUserController {

    private final MigrateUserDESTRUCTIVE migrateUserDESTRUCTIVE;
    private final UserService userService;

    public MigrateUserController(MigrateUserDESTRUCTIVE migrateUserDESTRUCTIVE, UserService userService) {
        this.migrateUserDESTRUCTIVE = migrateUserDESTRUCTIVE;
        this.userService = userService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<User> migrateUser(@PathVariable String userId) {
        // 1. Validate user exists
        User user = userService.fetchById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // 2. Check that roles are not equal to 9, 7 or 2
        // Role 2 seems to be the target (Member), 7 and 9 are admins.
        short roleId = user.getRole().getId();
        if (roleId == 9 || roleId == 7 || roleId == 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User role prevents migration (already Member or Admin)");
        }

        // 3. Trigger migration to role 2 (Member)
        User migratedUser = migrateUserDESTRUCTIVE.migrateUserRole(userId, (short) 2);

        return ResponseEntity.ok(migratedUser);
    }
}
