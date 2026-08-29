package com.clothing.ai.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.security.Principal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AuthenticatedUser implements Principal {
    private final UUID id;
    private final String email;
    private final String role;

    @Override public String getName() { return email; }
    public boolean isAdmin() { return "ADMIN".equals(role) || "STAFF".equals(role); }
}
