package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.UserProfileResponse;
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

    public UserController(UserService userService, JPAUserAccountsRepo userAccountsRepo, PaymentService paymentService, JPAUserSiteRepo userSiteRepo, UserSiteSubService userSiteSubService) {
        this.userService = userService;
        this.paymentService = paymentService;
        this.userSiteSubService = userSiteSubService;
    }

    @GetMapping(value = "/me", produces = "application/json")
    public ResponseEntity<UserProfileResponse> getCurrentUser(Authentication authentication) {
        String matricule = authentication.getName(); // JWT subject (matricule) is injected here
        return ResponseEntity.ok(fetchUserProfile(matricule));
    }

    @GetMapping(produces = "application/json")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> responses = userService.fetchAll().stream()
                .map(u -> fetchUserProfile(u.getMatricule()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value = "/{matricule}", produces = "application/json")
    public ResponseEntity<UserProfileResponse> getUserByMatricule(@PathVariable String matricule) {
        Optional<User> userOpt = userService.fetchById(matricule);
        if (userOpt.isEmpty()) {
            logger.warn("User with matricule {} not found", matricule);
            return ResponseEntity.notFound().build();
        }
        logger.info("User with matricule {} found", matricule);
        return ResponseEntity.ok(fetchUserProfile(matricule));
    }

    private UserProfileResponse fetchUserProfile(String matricule) {
        User u = userService.fetchById(matricule).orElseThrow();
        Optional<UserAccounts> acc = Optional.empty();
        List<UsersSites> sites = List.of();
        UserRoles userRole = u.getRole();
        String roleName = (userRole != null) ? userRole.getName() : null;
        try {
            if (roleName != null && (roleName.equalsIgnoreCase("ADMIN") || roleName.equalsIgnoreCase("ROLE_ADMIN"))) {
                // ADMIN: no payment account, no sites
                logger.info("User {} is ADMIN, skipping payment account and sites.", matricule);
            } else if (roleName != null && (roleName.equalsIgnoreCase("SITE_ADMIN") || roleName.equalsIgnoreCase("ROLE_SITE_ADMIN"))) {
                // SITE_ADMIN: only sites
                logger.info("User {} is SITE_ADMIN, skipping payment account.", matricule);
                sites = userSiteSubService.fetchByUser(matricule);
            } else {
                // Normal user: payment account and sites
                acc = paymentService.fetchUserAccount(matricule);
                sites = userSiteSubService.fetchByUser(matricule);
            }
        } catch (Exception e) {
            logger.warn("Exception while building profile for {}: {}", matricule, e.getMessage());
        }
        return UserProfileResponse.from(u, acc.orElse(null), sites);
    }
}
