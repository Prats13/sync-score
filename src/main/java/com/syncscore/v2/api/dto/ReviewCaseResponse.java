package com.syncscore.v2.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ReviewCaseResponse(
        UUID id,
        UUID agencyId,
        UUID architectureScanId,
        String status,
        String triggerReason,
        JsonNode triggerDetailsJson,
        UUID resolvedBy,
        String resolutionNote,
        Instant createdAt,
        Instant resolvedAt
) {}
