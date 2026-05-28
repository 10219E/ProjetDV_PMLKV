package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.SimpleInviteDto;
import lu.ephec.backend_projetdv2026.dto.compodto.UserProfileDto;
import lu.ephec.backend_projetdv2026.dto.compodto.UserPenaltyUpdateDto;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.models.UserAccounts;
import lu.ephec.backend_projetdv2026.models.UserRoles;
import lu.ephec.backend_projetdv2026.models.UsersSites;
import lu.ephec.backend_projetdv2026.models.UserPenalties;
import lu.ephec.backend_projetdv2026.services.PaymentService;
import lu.ephec.backend_projetdv2026.services.UserService;
import lu.ephec.backend_projetdv2026.repo.JPAUserAccountsRepo;
import lu.ephec.backend_projetdv2026.repo.JPAUserSiteRepo;
import lu.ephec.backend_projetdv2026.services.UserSiteSubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final PaymentService paymentService;
    private final UserSiteSubService userSiteSubService;
    private final PasswordEncoder passwordEncoder;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    // Prefer checking role IDs (stable) instead of string names which can vary/case issues
    private static final short ROLE_ADMIN_ID = 9;
    private static final short ROLE_SITE_ADMIN_ID = 7;

    public UserController(UserService userService, JPAUserAccountsRepo userAccountsRepo, PaymentService paymentService, JPAUserSiteRepo userSiteRepo, UserSiteSubService userSiteSubService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.paymentService = paymentService;
        this.userSiteSubService = userSiteSubService;
        this.passwordEncoder = passwordEncoder;
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

    @GetMapping(value = "/siteusers", produces = "application/json")
    public ResponseEntity<List<UserProfileDto>> getUsersForSite(@RequestParam("siteId") Integer siteId){
        List<UserProfileDto> responses = userService.fetchBySite(siteId).stream()
                .map(this::fetchUserProfile)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // Single endpoint to fetch by either email or matricule to avoid ambiguous mappings when both
    // routes would match the same path segment. If the identifier contains '@' it is treated as an email.
    @GetMapping(value = "/{identifier}", produces = "application/json")
    public ResponseEntity<UserProfileDto> getUserByIdentifier(@PathVariable String identifier) {
        if (identifier == null) {
            return ResponseEntity.notFound().build();
        }

        // Attempt to fetch by email or matricule. Treat NOT_FOUND as a normal condition (warning)
        // and only log errors for unexpected failures.
        if (identifier.contains("@")) {
            try {
                Optional<User> userOpt = userService.fetchByMail(identifier);
                if (userOpt.isEmpty()) {
                    logger.warn("[USER CONTROLLER] User with email {} not found", identifier);
                    return ResponseEntity.notFound().build();
                }
                logger.info("[USER CONTROLLER] User with email {} found", identifier);
                return ResponseEntity.ok(fetchUserProfile(userOpt.get()));
            } catch (HttpClientErrorException hce) {
                if (hce.getStatusCode() == HttpStatus.NOT_FOUND) {
                    logger.warn("[USER CONTROLLER] User with email {} not found: {}", identifier, hce.getMessage());
                    return ResponseEntity.notFound().build();
                }
                logger.error("[USER CONTROLLER] HTTP error while fetching user by email {}: {}", identifier, hce.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            } catch (ResponseStatusException rse) {
                // Some services throw ResponseStatusException; check message for not-found hints.
                final String msg = rse.getMessage() != null ? rse.getMessage() : "";
                if (msg.contains("404") || msg.contains("NOT_FOUND")) {
                    logger.warn("[USER CONTROLLER] User with email {} not found: {}", identifier, rse.getReason());
                    return ResponseEntity.notFound().build();
                }
                logger.error("[USER CONTROLLER] Error while fetching user by email {}: {}", identifier, rse.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            } catch (Exception e) {
                logger.error("[USER CONTROLLER] Unexpected error while fetching user by email {}: {}", identifier, e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            try {
                Optional<User> userOpt = userService.fetchById(identifier);
                if (userOpt.isEmpty()) {
                    logger.warn("[USER CONTROLLER] User with matricule {} not found", identifier);
                    return ResponseEntity.notFound().build();
                }
                logger.info("[USER CONTROLLER] User with matricule {} found", identifier);
                return ResponseEntity.ok(fetchUserProfile(userOpt.get()));
            } catch (HttpClientErrorException hce) {
                if (hce.getStatusCode() == HttpStatus.NOT_FOUND) {
                    logger.warn("[USER CONTROLLER] User with matricule {} not found: {}", identifier, hce.getMessage());
                    return ResponseEntity.notFound().build();
                }
                logger.error("[USER CONTROLLER] HTTP error while fetching user by matricule {}: {}", identifier, hce.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            } catch (ResponseStatusException rse) {
                final String msg = rse.getMessage() != null ? rse.getMessage() : "";
                if (msg.contains("404") || msg.contains("NOT_FOUND")) {
                    logger.warn("[USER CONTROLLER] User with matricule {} not found: {}", identifier, rse.getReason());
                    return ResponseEntity.notFound().build();
                }
                logger.error("[USER CONTROLLER] Error while fetching user by matricule {}: {}", identifier, rse.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            } catch (Exception e) {
                logger.error("[USER CONTROLLER] Unexpected error while fetching user by matricule {}: {}", identifier, e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @GetMapping(value = "/invite/{email}", produces = "application/json")
    public ResponseEntity<SimpleInviteDto> getUserByEmail(@PathVariable String email) {
        if (email == null) {
            return ResponseEntity.notFound().build();
        }

        // Fetch user by email
        Optional<User> userOptional = userService.fetchByMail(email);

        if (userOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOptional.get();

        // Check if user has active penalties
        boolean hasActivePenalties = userService.hasActivePenalty(user.getMatricule());

        // Create the DTO with only the required information
        SimpleInviteDto dto = new SimpleInviteDto(
                user.getEmail(),
                user.getMatricule(),
                user.getRole().getId(),
                hasActivePenalties
        );

        return ResponseEntity.ok(dto);
    }

    @PatchMapping(value = "/{userId}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<UserProfileDto> updateUser(@PathVariable String userId, @RequestBody Map<String, Object> updates) {
        logger.info("[USER CONTROLLER] Update request for user {}", userId);

        Optional<User> userOpt = userService.fetchById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User existingUser = userOpt.get();

        // Map updates to a partial User object for the service
        User updateData = new User();

        if (updates.containsKey("firstName")) {
            updateData.setFirstName((String) updates.get("firstName"));
        }
        if (updates.containsKey("lastName")) {
            updateData.setLastName((String) updates.get("lastName"));
        }
        if (updates.containsKey("email")) {
            updateData.setEmail((String) updates.get("email"));
        }
        if (updates.containsKey("birthDate")) {
            String bdate = (String) updates.get("birthDate");
            if (bdate != null) updateData.setBirthDate(LocalDate.parse(bdate));
        }
        if (updates.containsKey("level")) {
            updateData.setLevel((String) updates.get("level"));
        }
        if (updates.containsKey("isActive")) {
            updateData.setIsActive((Boolean) updates.get("isActive"));
        }

        // Handle Password Update (Mirroring UserRegistrationController logic)
        if (updates.containsKey("password")) {
            String rawPassword = (String) updates.get("password");
            if (rawPassword != null && !rawPassword.isBlank()) {
                updateData.setAuth(passwordEncoder.encode(rawPassword));
                logger.info("[USER CONTROLLER] User {} requested a password change", userId);
            }
        }

        return userService.updateUser(userId, updateData)
                .map(updated -> ResponseEntity.ok(fetchUserProfile(updated)))
                .orElse(ResponseEntity.badRequest().build());
    }

    @PatchMapping(value = "/pen/{userId}", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Void> updateUserPenaltyAndAccount(@PathVariable String userId, @RequestBody UserPenaltyUpdateDto dto) {
        logger.info("[USER CONTROLLER] Update penalty and account for user {}", userId);

        // 1. Update user's account balance
        UserAccounts account = paymentService.fetchUserAccount(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User account not found"));

        double previousBalance = account.getBalance() != null ? account.getBalance() : 0.0;
        double newBalance = previousBalance + dto.getAmount();
        paymentService.updateAccountStatus(userId, "clear", dto.getAmount());
        logger.info("[USER CONTROLLER] Updated user {} balance from {} EUR to {} EUR", userId, previousBalance, newBalance);

        // 2. Deactivate the penalty
        UserPenalties penaltyUpdate = new UserPenalties();
        penaltyUpdate.setIsActive(false);
        userService.updatePenalty(dto.getPenaltyId(), penaltyUpdate);
        logger.info("[USER CONTROLLER] Deactivated penalty {} for user {}", dto.getPenaltyId(), userId);

        return ResponseEntity.ok().build();
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
                logger.info("[USER CONTROLLER] User {} is SUPER_ADMIN (roleId={}), skipping payment account and sites.", matricule, roleId);
            } else if (roleId != null && roleId.equals(ROLE_SITE_ADMIN_ID)) {
                // SITE_ADMIN: only sites
                logger.info("[USER CONTROLLER] User {} is SITE_ADMIN (roleId={}), skipping payment account.", matricule, roleId);
                sites = userSiteSubService.fetchByUser(matricule);
            } else {
                // Normal user: payment account and sites
                try {
                    acc = paymentService.fetchUserAccount(matricule);
                } catch (Exception ex) {
                    // keep behavior: log and continue with null account
                    logger.warn("[USER CONTROLLER] No payment account for user {}: {}", matricule, ex.getMessage());
                }
                sites = userSiteSubService.fetchByUser(matricule);
            }
        } catch (Exception e) {
            logger.warn("[USER CONTROLLER] Exception while building profile for {}: {}", matricule, e.getMessage());
        }
        return UserProfileDto.from(u, acc.orElse(null), sites);
    }
}
