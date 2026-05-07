package lu.ephec.backend_projetdv2026.services.security;

import lu.ephec.backend_projetdv2026.models.EnumUserRolesType;
import lu.ephec.backend_projetdv2026.models.User;
import lu.ephec.backend_projetdv2026.repo.JPAUserRepo;
import lu.ephec.backend_projetdv2026.services.availability.AvailabilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class UserConfigService implements UserDetailsService {

    private final JPAUserRepo jpaUserRepo;

    private static final Logger logger = LoggerFactory.getLogger(UserConfigService.class);

    public UserConfigService(JPAUserRepo jpaUserRepo) {
        this.jpaUserRepo = jpaUserRepo;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        String reclogin = login == null ? "" : login.trim();

        User dbUser = jpaUserRepo.findByEmail(reclogin.toLowerCase())
                .or(() -> jpaUserRepo.findById(reclogin.toUpperCase(Locale.ROOT))) // matricule fallback
                .orElseThrow(() -> new UsernameNotFoundException("User not found with matricule or email: " + login));


        if (dbUser.getAuth() == null || dbUser.getAuth().isBlank()) { //BCrypt Hash stored for each user
            logger.error("[Service - UserConfigService] No password configured for user: {}", reclogin);
            throw new UsernameNotFoundException("No password configured for user: " + reclogin);
        }

        Short roleId = dbUser.getRole() != null ? dbUser.getRole().getId() : null; //Using ENUM for roles to avoid LAZY loading issues and simplify authority mapping
        EnumUserRolesType roleType = roleId != null ? EnumUserRolesType.fromId(roleId) : null;

        String authority = roleType != null
                ? "ROLE_" + roleType.name()
                : "ROLE_USER";

        logger.info("[Service - UserConfigService] User {} has role {}", dbUser.getMatricule(), authority);

        return org.springframework.security.core.userdetails.User.builder()
                .username(dbUser.getMatricule())              // login field by Matricule (even if email was used for login)
                .password(dbUser.getAuth())               // BCrypt hash from DB
                .authorities(List.of(new SimpleGrantedAuthority(authority)))
                .accountLocked(false)
                .disabled(Boolean.FALSE.equals(dbUser.getIsActive()))
                .build();
    }
}