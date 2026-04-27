package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.UserProfileDto;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.models.UserAccounts;
import lu.ephec.backend_projetdv2026.models.UserRoles;
import lu.ephec.backend_projetdv2026.models.UsersSites;
import lu.ephec.backend_projetdv2026.services.PaymentService;
import lu.ephec.backend_projetdv2026.services.UserService;
import lu.ephec.backend_projetdv2026.repo.JPAUserAccountsRepo;
import lu.ephec.backend_projetdv2026.repo.JPAUserSiteRepo;
import lu.ephec.backend_projetdv2026.services.UserSiteSubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PaymentService paymentService;
    private final UserSiteSubService userSiteSubService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    // Prefer checking role IDs (stable) instead of string names which can vary/case issues
    private static final short ROLE_ADMIN_ID = 9;
    private static final short ROLE_SITE_ADMIN_ID = 7;

    public UserController(UserService userService, JPAUserAccountsRepo userAccountsRepo, PaymentService paymentService, JPAUserSiteRepo userSiteRepo, UserSiteSubService userSiteSubService) {
        this.userService = userService;
        this.paymentService = paymentService;
        this.userSiteSubService = userSiteSubService;
    }

    @GetMapping(value = "/me", produces = "application/json")
    public ResponseEntity<UserProfileDto> getCurrentUser(Authentication authentication) {
        String matricule = authentication.getName(); // JWT subject (matricule) is injected here
        User u = userService.fetchById(matricule).orElseThrow();
        return ResponseEntity.ok(fetchUserProfile(u));
    }

    @GetMapping(produces = "application/json")
    public ResponseEntity<List<UserProfileDto>> getAllUsers() {
        List<UserProfileDto> responses = userService.fetchAll().stream()
                .map(this::fetchUserProfile)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value= "/{email}", produces = "application/json")
    public ResponseEntity<UserProfileDto> getUserByEmail(@PathVariable String email) {
        Optional<User> userOpt = userService.fetchByMail(email);
        if (userOpt.isEmpty()) {
            logger.warn("User with email {} not found", email);
            return ResponseEntity.notFound().build();
        }
        logger.info("User with email {} found", email);
        return ResponseEntity.ok(fetchUserProfile(userOpt.get()));
    }

    @GetMapping(value = "/{matricule}", produces = "application/json")
    public ResponseEntity<UserProfileDto> getUserByMatricule(@PathVariable String matricule) {
        Optional<User> userOpt = userService.fetchById(matricule);
        if (userOpt.isEmpty()) {
            logger.warn("User with matricule {} not found", matricule);
            return ResponseEntity.notFound().build();
        }
        logger.info("User with matricule {} found", matricule);
        return ResponseEntity.ok(fetchUserProfile(userOpt.get()));
    }

    private UserProfileDto fetchUserProfile(User u) {
        String matricule = u.getMatricule();
        Optional<UserAccounts> acc = Optional.empty();
        List<UsersSites> sites = List.of();
        UserRoles userRole = u.getRole();
        Short roleId = (userRole != null) ? userRole.getId() : null;
        try {
            if (roleId != null && roleId.shortValue() == ROLE_ADMIN_ID) {
                // ADMIN: no payment account, no sites
                logger.info("User {} is ADMIN (roleId={}), skipping payment account and sites.", matricule, roleId);
            } else if (roleId != null && roleId.equals(ROLE_SITE_ADMIN_ID)) {
                // SITE_ADMIN: only sites
                logger.info("User {} is SITE_ADMIN (roleId={}), skipping payment account.", matricule, roleId);
                sites = userSiteSubService.fetchByUser(matricule);
            } else {
                // Normal user: payment account and sites
                try {
                    acc = paymentService.fetchUserAccount(matricule);
                } catch (Exception ex) {
                    // keep behavior: log and continue with null account
                    logger.warn("No payment account for user {}: {}", matricule, ex.getMessage());
                }
                sites = userSiteSubService.fetchByUser(matricule);
            }
        } catch (Exception e) {
            logger.warn("Exception while building profile for {}: {}", matricule, e.getMessage());
        }
        return UserProfileDto.from(u, acc.orElse(null), sites);
    }
}
