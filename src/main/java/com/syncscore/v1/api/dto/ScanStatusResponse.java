package com.syncscore.v1.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.syncscore.v1.domain.ManifestType;
import com.syncscore.v1.domain.RepositoryScanStatus;
import com.syncscore.v1.domain.ScanStatus;
import com.syncscore.v1.domain.ScanTriggerType;
import com.syncscore.v1.domain.SyncTier;
import com.syncscore.v1.domain.VerificationLabel;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ScanStatusResponse(
        UUID scanId,
        UUID agencyId,
        ScanTriggerType triggerType,
        ScanStatus status,
        String errorMessage,
        String rulesetVersion,
        VerificationLabel verificationLabel,
        JsonNode evidenceItemIds,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt,
        Score score,
        List<Repository> repositories,
        List<Detected> detectedPackages
) {
    public record Score(
            int totalScore,
            SyncTier tier,
            JsonNode categorySubtotals
    ) {}

    public record Repository(
            UUID repositoryScanId,
            String repoFullName,
            String repoUrl,
            String defaultBranch,
            RepositoryScanStatus status,
            JsonNode manifestsFound
    ) {}

    public record Detected(
            String packageName,
            String category,
            int pointsAwarded,
            ManifestType manifestType,
            String manifestPath,
            UUID repositoryScanId
    ) {}
}

