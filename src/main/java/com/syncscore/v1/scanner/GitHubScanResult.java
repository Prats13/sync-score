package com.syncscore.v1.scanner;

import com.syncscore.v1.domain.ManifestType;
import java.util.List;
import java.util.Set;

public record GitHubScanResult(
        String username,
        int repoLimitApplied,
        List<ScannedRepo> scannedRepos,
        Set<String> packagesFound
){
    public record ScannedRepo(
            String owner,
            String name,
            String htmlUrl,
            String defaultBranch,
            List<ManifestFile> manifestsFound
    ) {}

    public record ManifestFile(
            ManifestType type,
            String path,
            Set<String> packages
    ) {}
}
