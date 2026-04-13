package com.syncscore.producer.service.dto;

import java.time.Instant;
import java.util.UUID;

public record ProducerProfileResponse(
        UUID id,
        String linkedinUrl,
        String githubUrl,
        String websiteUrl,
        String liveProjectUrl,
        String badge2Status,
        VerificationResult verification,
        Instant verifiedAt,
        Instant createdAt
) {
    public record VerificationResult(
            Boolean linkedinReachable,
            Boolean githubReachable,
            Boolean websiteReachable,
            Boolean liveProjectReachable
    ) {}
}