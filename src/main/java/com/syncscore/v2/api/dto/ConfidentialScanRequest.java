package com.syncscore.v2.api.dto;

import java.util.List;

public record ConfidentialScanRequest(
        String source,
        List<String> exclusions,
        String customExclusions
) {}
