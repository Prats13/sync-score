package com.syncscore.security;

import com.syncscore.auth.domain.Role;
import java.util.Set;
import java.util.UUID;

public record AccessPrincipal(
        UUID userId,
        String username,
        Set<Role> roles
) {}

