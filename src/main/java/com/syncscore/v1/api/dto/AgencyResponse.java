package com.syncscore.v1.api.dto;

import java.time.Instant;
import java.util.UUID;

public record AgencyResponse(
        UUID agencyId,
        String name,
        String niche,
        String websiteUrl,
        String description,
        String bookingUrl,
        String githubUsername,
        boolean isPublic,
        String publicSlug,
        int rescanCount,
        int rescanLimit,
        int repoScanLimit,
        Instant createdAt,
        Instant updatedAt
) {}

