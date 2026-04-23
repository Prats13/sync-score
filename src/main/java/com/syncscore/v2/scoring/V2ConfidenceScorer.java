package com.syncscore.v2.scoring;

import com.syncscore.v2.domain.ArchConfidence;
import com.syncscore.v2.domain.ArchStatus;
import com.syncscore.v2.domain.StructuralSignalType;
import com.syncscore.v2.scanner.StructuralSignalAggregator.AggregatedSignals;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class V2ConfidenceScorer {

    // Weights must sum to 100
    private static final double W_COMMIT = 15.0;
    private static final double W_CONTRIBUTOR = 15.0;
    private static final double W_AGE = 10.0;
    private static final double W_DEPTH = 30.0;
    private static final double W_CONSISTENCY = 30.0;

    private static final int CONSISTENCY_MISMATCH_THRESHOLD = 30;

    public Result score(AggregatedSignals signals, boolean antiGamingFired) {
        List<SignalScore> scores = new ArrayList<>();

        double commitNorm = normalizeCommits(signals.totalCommits90d());
        scores.add(new SignalScore(StructuralSignalType.COMMIT_FREQUENCY,
                BigDecimal.valueOf(signals.totalCommits90d()),
                labelCommits(signals.totalCommits90d()),
                BigDecimal.valueOf(commitNorm * W_COMMIT / 100.0)));

        double contribNorm = normalizeContributors(signals.maxContributors());
        scores.add(new SignalScore(StructuralSignalType.CONTRIBUTOR_COUNT,
                BigDecimal.valueOf(signals.maxContributors()),
                labelContributors(signals.maxContributors()),
                BigDecimal.valueOf(contribNorm * W_CONTRIBUTOR / 100.0)));

        double ageNorm = normalizeAge(signals.oldestRepoAgeMonths());
        scores.add(new SignalScore(StructuralSignalType.REPO_AGE_MONTHS,
                BigDecimal.valueOf(signals.oldestRepoAgeMonths()),
                labelAge(signals.oldestRepoAgeMonths()),
                BigDecimal.valueOf(ageNorm * W_AGE / 100.0)));

        double depthNorm = normalizeDepth(signals.maxFolderDepth(), signals.totalServices());
        scores.add(new SignalScore(StructuralSignalType.FOLDER_DEPTH,
                BigDecimal.valueOf(signals.maxFolderDepth()),
                labelDepth(signals.maxFolderDepth(), signals.totalServices()),
                BigDecimal.valueOf(depthNorm * W_DEPTH / 100.0)));

        // Persist service count for anti-gaming diff / admin review; not directly weighted (depth score covers it).
        scores.add(new SignalScore(StructuralSignalType.SERVICE_COUNT,
                BigDecimal.valueOf(signals.totalServices()),
                labelServices(signals.totalServices()),
                BigDecimal.ZERO));

        double consistencyNorm = signals.manifestConsistencyScore();
        scores.add(new SignalScore(StructuralSignalType.MANIFEST_CONSISTENCY,
                BigDecimal.valueOf(signals.manifestConsistencyScore()),
                labelConsistency(signals.manifestConsistencyScore()),
                BigDecimal.valueOf(consistencyNorm * W_CONSISTENCY / 100.0)));

        double total = scores.stream()
                .mapToDouble(s -> s.weightedContribution().doubleValue())
                .sum();

        ArchConfidence confidence = total >= 70 ? ArchConfidence.HIGH
                : total >= 40 ? ArchConfidence.MEDIUM
                : ArchConfidence.LOW;

        ArchStatus status;
        if (antiGamingFired) {
            status = ArchStatus.UNDER_REVIEW;
        } else if (signals.manifestConsistencyScore() < CONSISTENCY_MISMATCH_THRESHOLD) {
            status = ArchStatus.EVIDENCE_MISMATCH;
        } else if (confidence == ArchConfidence.LOW) {
            status = ArchStatus.FRESHNESS_LOW;
        } else {
            status = ArchStatus.VERIFIED;
        }

        return new Result(confidence, status, scores, total);
    }

    private double normalizeCommits(int count) {
        if (count >= 20) return 100;
        if (count >= 5) return 50;
        return 0;
    }

    private double normalizeContributors(int count) {
        if (count >= 3) return 100;
        if (count >= 2) return 50;
        return 0;
    }

    private double normalizeAge(int months) {
        if (months >= 6) return 100;
        if (months >= 2) return 50;
        return 0;
    }

    private double normalizeDepth(int depth, int services) {
        if (depth >= 4 || services >= 3) return 100;
        if (depth >= 2 || services >= 2) return 50;
        return 0;
    }

    private String labelCommits(int count) {
        return count >= 20 ? "HIGH" : count >= 5 ? "MEDIUM" : "LOW";
    }

    private String labelContributors(int count) {
        return count >= 3 ? "HIGH" : count >= 2 ? "MEDIUM" : "LOW";
    }

    private String labelAge(int months) {
        return months >= 6 ? "HIGH" : months >= 2 ? "MEDIUM" : "LOW";
    }

    private String labelDepth(int depth, int services) {
        return (depth >= 4 || services >= 3) ? "HIGH" : (depth >= 2 || services >= 2) ? "MEDIUM" : "LOW";
    }

    private String labelServices(int services) {
        return services >= 3 ? "HIGH" : services >= 2 ? "MEDIUM" : "LOW";
    }

    private String labelConsistency(int score) {
        return score >= 80 ? "HIGH" : score >= 50 ? "MEDIUM" : "LOW";
    }

    public record Result(
            ArchConfidence confidence,
            ArchStatus archStatus,
            List<SignalScore> signalScores,
            double weightedTotal
    ) {}
}
