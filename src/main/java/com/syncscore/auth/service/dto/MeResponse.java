package com.syncscore.auth.service.dto;

import com.syncscore.auth.domain.Role;
import java.util.Set;
import java.util.UUID;

public record MeResponse(
        UUID userId,
        String username,
        Set<Role> roles
) {}

