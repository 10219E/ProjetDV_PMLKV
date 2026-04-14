package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.UserProfileResponse;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.models.UserAccounts;
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
        return ResponseEntity.ok(buildUserProfile(matricule));
    }

    @GetMapping(produces = "application/json")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        List<UserProfileResponse> responses = userService.fetchAll().stream()
                .map(u -> buildUserProfile(u.getMatricule()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping(value = "/{matricule}", produces = "application/json")
    public ResponseEntity<UserProfileResponse> getUserByMatricule(@PathVariable String matricule) {
        return ResponseEntity.ok(buildUserProfile(matricule));
    }

    private UserProfileResponse buildUserProfile(String matricule) {
        User u = userService.fetchById(matricule).orElseThrow();
        Optional<UserAccounts> acc = paymentService.fetchUserAccount(matricule);
        List<UsersSites> sites = userSiteSubService.fetchByUser(matricule);
        return UserProfileResponse.from(u, acc.orElse(null), sites);
    }
}
