package com.syncscore.v1.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.syncscore.v1.domain.SyncTier;
import com.syncscore.v1.domain.VerificationLabel;
import java.util.List;
import java.util.UUID;

public record PublicAgencyProfileResponse(
        UUID agencyId,
        String slug,
        VerificationLabel verificationLabel,
        Agency agency,
        Score score,
        List<StackChip> stack
) {
    public record Agency(
            String name,
            String niche,
            String websiteUrl,
            String description,
            String bookingUrl
    ) {}

    public record Score(
            Integer totalScore,
            SyncTier tier,
            String rulesetVersion,
            JsonNode categorySubtotals
    ) {}

    public record StackChip(
            String packageName,
            String category,
            int pointsAwarded
    ) {}
}

