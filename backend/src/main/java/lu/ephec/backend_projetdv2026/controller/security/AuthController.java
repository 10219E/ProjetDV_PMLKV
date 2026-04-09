package lu.ephec.backend_projetdv2026.controller.security;

import lu.ephec.backend_projetdv2026.dto.security.AuthLoginDto;
import lu.ephec.backend_projetdv2026.dto.security.AuthLoginResponse;
import lu.ephec.backend_projetdv2026.services.UserService;
import lu.ephec.backend_projetdv2026.services.security.JWTService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JWTService jwtService;
    private final UserService userService;

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

            userService.touchLastLoginByMatricule(principal.getUsername()); // Update last login time

            String token = jwtService.generateToken(principal);

            return ResponseEntity.ok(
                    new AuthLoginResponse("Bearer", token, jwtService.getExpirationSeconds())
            );
        } catch (BadCredentialsException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
    }
}