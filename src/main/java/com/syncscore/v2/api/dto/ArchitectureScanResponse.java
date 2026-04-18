package com.syncscore.v2.api.dto;

import com.syncscore.v2.domain.ArchConfidence;
import com.syncscore.v2.domain.ArchStatus;
import java.time.Instant;
import java.util.UUID;

public record ArchitectureScanResponse(
        UUID id,
        UUID agencyId,
        String status,
        ArchConfidence confidence,
        ArchStatus archStatus,
        String evidenceSource,
        String rulesetVersion,
        Instant createdAt,
        Instant finishedAt
) {}
