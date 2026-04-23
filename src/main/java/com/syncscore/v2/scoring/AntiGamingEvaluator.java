package com.syncscore.v2.scoring;

import com.syncscore.v2.scanner.StructuralSignalAggregator.AggregatedSignals;
import org.springframework.stereotype.Component;

@Component
public class AntiGamingEvaluator {

    public Result evaluate(AggregatedSignals prior, AggregatedSignals current,
                           int newHighTierPackages, int recentCommits30d) {
        // Heuristic 4 first: new repo with mature claims (age=0 months)
        if (current.oldestRepoAgeMonths() == 0
                && current.totalServices() >= 3
                && newHighTierPackages >= 5) {
            return new Result(true, "NEW_REPO_MATURE_CLAIMS",
                    "Repo age is 0 months but claims " + current.totalServices() + " services and "
                            + newHighTierPackages + " high-tier packages");
        }

        // Heuristic 1: package spike without structural change
        boolean noStructuralChange = current.maxFolderDepth() == prior.maxFolderDepth()
                && current.totalServices() == prior.totalServices();
        if (newHighTierPackages >= 5 && noStructuralChange) {
            return new Result(true, "HIGH_TIER_PACKAGES_NO_STRUCTURAL_CHANGE",
                    newHighTierPackages + " new high-tier packages with no structural change");
        }

        // Heuristic 2: score jump without activity
        boolean priorWasLow = prior.totalCommits90d() <= 3 && prior.maxContributors() <= 1;
        boolean currentIsHigh = current.totalCommits90d() >= 20 && current.maxContributors() >= 3;
        if (priorWasLow && currentIsHigh && recentCommits30d <= 2) {
            return new Result(true, "SCORE_JUMP_WITHOUT_ACTIVITY",
                    "Confidence jumped high but only " + recentCommits30d + " commits in last 30 days");
        }

        // Heuristic 3: consistency collapse
        int consistencyDrop = prior.manifestConsistencyScore() - current.manifestConsistencyScore();
        if (consistencyDrop > 40) {
            return new Result(true, "CONSISTENCY_COLLAPSE",
                    "Manifest consistency dropped " + consistencyDrop + " points");
        }

        return new Result(false, null, null);
    }

    public record Result(boolean fired, String reason, String detail) {}
}
