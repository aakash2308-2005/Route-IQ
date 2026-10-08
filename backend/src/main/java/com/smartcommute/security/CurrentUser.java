package com.smartcommute.security;

import com.smartcommute.web.error.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper to obtain the authenticated user id from the security context. Centralizes the "who is
 * calling" logic so controllers/services don't reach into Spring Security directly.
 */
@Component
public class CurrentUser {

    /** The authenticated user's id, or throws {@link UnauthorizedException} if not authenticated. */
    public long requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("authentication required");
        }
        return user.userId();
    }
}
