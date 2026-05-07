package lu.ephec.backend_projetdv2026.services.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class JWTAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthFilter.class);
    private final JWTService jwtService;
    private final UserConfigService userConfigService;

    public JWTAuthFilter(JWTService jwtService, UserConfigService userConfigService) {
        this.jwtService = jwtService;
        this.userConfigService = userConfigService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        logger.info("[Service - JWT AUTH] Processing: {} {}", request.getMethod(), request.getRequestURI());
        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("[Service - JWT AUTH] No Bearer token found, skipping authentication");
            filterChain.doFilter(request, response);
            return;
        }

        final String token = authHeader.substring(7).trim();

        try {
            final String login = jwtService.extractLogin(token);

            if (login != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userConfigService.loadUserByUsername(login);
                logger.info("[Service - JWT AUTH] Loaded user details for: {}", userDetails != null ? userDetails.getUsername() : "null");

                if (jwtService.isTokenValid(token, userDetails)) {
                    logger.info("[Service - JWT AUTH] Token is valid for user: {}", login);
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    logger.warn("[Service - JWT AUTH] Token is NOT valid for user: {}", login);
                }
            }
        } catch (JwtException | IllegalArgumentException e) {
            logger.error("[Service - JWT AUTH] Exception during token validation: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
