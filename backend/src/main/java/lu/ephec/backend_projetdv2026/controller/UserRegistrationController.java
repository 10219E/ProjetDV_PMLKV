package lu.ephec.backend_projetdv2026.controller;

import lu.ephec.backend_projetdv2026.dto.UserRegistrationDto;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.models.UserRoles;
import lu.ephec.backend_projetdv2026.services.UserService;
import lu.ephec.backend_projetdv2026.services.SiteService;
import lu.ephec.backend_projetdv2026.services.UserSiteSubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/auth")
public class UserRegistrationController {

	private final UserService userService;
	private final PasswordEncoder passwordEncoder;
	private final SiteService siteService;
	private final UserSiteSubService userSiteSubService;

	private static final Logger logger = LoggerFactory.getLogger(UserRegistrationController.class);

	public UserRegistrationController(UserService userService, PasswordEncoder passwordEncoder, SiteService siteService, UserSiteSubService userSiteSubService) {
		this.userService = userService;
		this.passwordEncoder = passwordEncoder;
		this.siteService = siteService;
		this.userSiteSubService = userSiteSubService;
	}

	@PostMapping(value = "/register", produces = "application/json")
	public ResponseEntity<Map<String,String>> register(@RequestBody UserRegistrationDto dto) {
		logger.info("Registration request received for email={}", dto != null ? dto.getEmail() : null);
		// basic validation
					if (dto == null || dto.getFname() == null || dto.getFname().isBlank() || dto.getLname() == null || dto.getLname().isBlank()
								|| dto.getEmail() == null || dto.getEmail().isBlank() || dto.getPassword() == null || dto.getPassword().isBlank()) {
							logger.warn("Registration failed: missing required fields (email={})", dto != null ? dto.getEmail() : null);
							throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing required fields");
						}

						// names should not contain symbols: allow letters (including accented), spaces, apostrophe and hyphen
						Pattern namePattern = Pattern.compile("^[A-Za-zÀ-ÖØ-öø-ÿ' -]+$");
						if (!namePattern.matcher(dto.getFname()).matches() || !namePattern.matcher(dto.getLname()).matches()) {
							logger.warn("Registration failed: invalid characters in names (email={})", dto.getEmail());
							throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "First name and last name contain invalid characters");
						}

		try {
			User u = new User();
			u.setIsActive(true);
			u.setFirstName(dto.getFname());
			u.setLastName(dto.getLname());
			u.setEmail(dto.getEmail());
			if (dto.getBdate() != null && !dto.getBdate().isBlank()) {
				u.setBirthDate(LocalDate.parse(dto.getBdate()));
			}
			u.setLevel(dto.getLvl());
			u.setAuth(passwordEncoder.encode(dto.getPassword()));
			u.setCreated(LocalDateTime.now());

			Short roleId = dto.getRoleId() != null ? dto.getRoleId() : 1;
			UserRoles r = new UserRoles();
			r.setId(roleId);
			u.setRole(r);

			User saved = userService.newUser(u);

			// If client provided a siteId, try to link user to that site by id
			if (dto.getSiteId() != null) {
				var maybeSite = siteService.fetchById(dto.getSiteId());
				if (maybeSite.isPresent()) {
					try {
						userSiteSubService.newUserSite(saved.getMatricule(), maybeSite.get().getSiteId(), true, false);
					} catch (Exception e) {
						logger.warn("Failed to link user {} to site id {}: {}", saved.getMatricule(), dto.getSiteId(), e.getMessage());
						// do not fail registration because linking failed; return created anyway
					}
				} else {
					logger.warn("Site not found for id='{}' while registering user={}", dto.getSiteId(), dto.getEmail());
					// we'll treat missing site as non-fatal: user is created but not linked
				}
			}
			logger.info("User registered: email={} matricule={} site={}", dto.getEmail(), saved.getMatricule(), siteService.fetchById(dto.getSiteId()).map(s -> s.getName()).orElse("N/A"));
			return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("matricule", saved.getMatricule()));
		} catch (Exception ex) {
			logger.error("Registration error for email={}", dto != null ? dto.getEmail() : null, ex);
			// let ValidationBoiler throw specific messages; wrap unexpected in 400
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
	}
}
