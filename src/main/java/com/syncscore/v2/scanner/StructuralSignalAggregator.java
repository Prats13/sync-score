package com.syncscore.v2.scanner;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StructuralSignalAggregator {

    private final ManifestConsistencyChecker consistencyChecker;

    public StructuralSignalAggregator(ManifestConsistencyChecker consistencyChecker) {
        this.consistencyChecker = consistencyChecker;
    }

    public AggregatedSignals aggregate(List<RepoStructuralSignals> repoSignals) {
        if (repoSignals == null || repoSignals.isEmpty()) {
            return new AggregatedSignals(0, 0, 0, 0, 0, 0, 0);
        }

        int totalCommits30d = 0;
        int totalCommits = 0;
        int maxContributors = 0;
        long oldestRepoAgeMonths = 0;
        int maxDepth = 0;
        int totalServices = 0;
        int totalSourceFiles = 0;
        int totalPackages = 0;

        Instant now = Instant.now();
        for (RepoStructuralSignals s : repoSignals) {
            totalCommits30d += s.commitCount30d();
            totalCommits += s.commitCount90d();
            if (s.contributorCount() > maxContributors) maxContributors = s.contributorCount();
            if (s.repoCreatedAt() != null) {
                long ageMonths = Math.max(0, ChronoUnit.MONTHS.between(s.repoCreatedAt(), now));
                if (ageMonths > oldestRepoAgeMonths) oldestRepoAgeMonths = ageMonths;
            }
            if (s.maxFolderDepth() > maxDepth) maxDepth = s.maxFolderDepth();
            totalServices += s.serviceCount();
            totalSourceFiles += s.sourceFileCount();
            totalPackages += s.detectedPackageCount();
        }

        int consistencyScore = consistencyChecker.computeConsistencyScore(totalSourceFiles, totalPackages);

        return new AggregatedSignals(
                totalCommits30d,
                totalCommits,
                maxContributors,
                (int) oldestRepoAgeMonths,
                maxDepth,
                totalServices,
                consistencyScore
        );
    }

    public record AggregatedSignals(
            int totalCommits30d,
            int totalCommits90d,
            int maxContributors,
            int oldestRepoAgeMonths,
            int maxFolderDepth,
            int totalServices,
            int manifestConsistencyScore
    ) {}
}
