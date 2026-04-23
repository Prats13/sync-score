package com.syncscore.v1.api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.syncscore.v1.domain.EvidenceType;
import jakarta.validation.constraints.NotNull;

public record EvidenceCreateRequest(
        @NotNull EvidenceType evidenceType,
        String contentText,
        String contentUrl,
        JsonNode payload
) {}

