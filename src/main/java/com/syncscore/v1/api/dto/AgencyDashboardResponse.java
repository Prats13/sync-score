package com.syncscore.v1.api.dto;

import com.syncscore.v1.domain.ScanStatus;
import com.syncscore.v1.domain.ScanTriggerType;
import com.syncscore.v1.domain.SyncTier;
import com.syncscore.v1.domain.VerificationLabel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AgencyDashboardResponse(
        AgencyResponse agency,
        LatestScore latestScore,
        List<ScanSummary> scans
) {
    public record LatestScore(
            Integer totalScore,
            SyncTier tier,
            VerificationLabel verificationLabel,
            UUID scanId,
            String rulesetVersion,
            Instant createdAt
    ) {}

    public record ScanSummary(
            UUID scanId,
            ScanTriggerType triggerType,
            ScanStatus status,
            VerificationLabel verificationLabel,
            Instant createdAt
    ) {}
}

