package com.smartcommute.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the {@code Authorization: Bearer <token>} header, verifies the JWT, and populates the
 * Spring Security context with the authenticated user id (docs/04-lld.md section 7).
 *
 * <p>On a missing or invalid token the filter simply does not authenticate the request; the
 * authorization rules in {@link SecurityConfig} then produce a 401 for protected endpoints. The
 * filter never throws to the client, avoiding leaking token-parsing details.</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Claims claims = jwtService.verify(token);
                long userId = Long.parseLong(claims.getSubject());
                AuthenticatedUser principal = new AuthenticatedUser(userId, claims.get("email", String.class));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, null,
                                AuthorityUtils.createAuthorityList("ROLE_USER"));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | NumberFormatException ex) {
                // Invalid/expired token -> leave context unauthenticated; do not leak details.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
