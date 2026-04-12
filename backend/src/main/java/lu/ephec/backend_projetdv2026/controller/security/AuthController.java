package lu.ephec.backend_projetdv2026.controller.security;

// ...existing imports...
import lu.ephec.backend_projetdv2026.dto.security.AuthLoginDto;
import lu.ephec.backend_projetdv2026.dto.security.AuthLoginResponse;
import lu.ephec.backend_projetdv2026.services.UserService;
import lu.ephec.backend_projetdv2026.services.security.JWTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(AuthenticationManager authenticationManager, JWTService jwtService, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthLoginResponse> login(@RequestBody AuthLoginDto request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getLogin(),
                            request.getPassword()
                    )
            );

            UserDetails principal = (UserDetails) authentication.getPrincipal();

            if (principal == null) {
                logger.error("Authentication succeeded but principal is null for login {}", request.getLogin());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Authentication principal is null");
            }

            String username = principal != null ? principal.getUsername() : null;
            if (username != null) {
                userService.touchLastLoginByMatricule(username); // Update last login time
                logger.info("User {} logged in at {} with email {}", username, LocalDateTime.now(), request.getLogin());
            }

            String token = jwtService.generateToken(principal);

            return ResponseEntity.ok(new AuthLoginResponse("Bearer", token, jwtService.getExpirationSeconds()));
        } catch (BadCredentialsException ex) {
            logger.error("User tried to connect with email "+ request.getLogin(), ex);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }
}