package com.syncscore.v1.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AgencyUpsertRequest(
        @NotBlank String name,
        String niche,
        String websiteUrl,
        String description,
        String bookingUrl,
        String githubUsername,
        Boolean isPublic
) {}

