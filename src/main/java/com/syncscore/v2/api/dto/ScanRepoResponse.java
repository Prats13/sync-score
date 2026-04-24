package com.syncscore.v2.api.dto;

public record ScanRepoResponse(
        String repoFullName,
        int commits30d,
        int commits90d,
        int contributorCount,
        int maxFolderDepth,
        int serviceCount,
        int sourceFileCount,
        int repoAgeMonths
) {}