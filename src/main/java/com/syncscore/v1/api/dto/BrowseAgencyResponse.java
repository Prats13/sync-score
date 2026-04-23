package com.syncscore.v1.api.dto;

import com.syncscore.v1.domain.SyncTier;
import com.syncscore.v1.domain.VerificationLabel;
import java.util.UUID;

public record BrowseAgencyResponse(
        String slug,
        UUID agencyId,
        String name,
        String niche,
        String websiteUrl,
        String bookingUrl,
        Integer score,
        SyncTier tier,
        VerificationLabel verificationLabel
) {}

