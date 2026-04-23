package com.syncscore.v2.api.dto;

import com.syncscore.v2.domain.ArchConfidence;
import com.syncscore.v2.domain.ArchStatus;
import java.time.Instant;
import java.util.UUID;

public record ArchitectureProfileResponse(
        UUID agencyId,
        ArchConfidence confidence,
        ArchStatus archStatus,
        String evidenceSource,
        boolean hasOpenReviewCase,
        Instant lastVerifiedAt
) {}
