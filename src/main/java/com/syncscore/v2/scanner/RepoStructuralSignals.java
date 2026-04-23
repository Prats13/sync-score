package com.syncscore.v2.scanner;

import java.time.Instant;

public record RepoStructuralSignals(
        String repoFullName,
        int commitCount30d,
        int commitCount90d,
        int contributorCount,
        Instant repoCreatedAt,
        int maxFolderDepth,
        int serviceCount,
        int sourceFileCount,
        int detectedPackageCount
) {}
