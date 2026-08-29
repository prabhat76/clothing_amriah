package com.clothing.ai.security;

import com.clothing.ai.common.exception.UnauthorizedException;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.UUID;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static AuthenticatedUser currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser u))
            throw new UnauthorizedException("Authentication required");
        return u;
    }

    public static UUID currentUserId() { return currentUser().getId(); }

    public static boolean isAuthenticated() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getPrincipal() instanceof AuthenticatedUser;
    }
}
