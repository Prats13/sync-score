package com.syncscore.v2.scoring;

import com.syncscore.v2.scanner.StructuralSignalAggregator.AggregatedSignals;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AntiGamingEvaluatorTest {

    private final AntiGamingEvaluator evaluator = new AntiGamingEvaluator();

    // Heuristic 1: many new high-tier packages but no structural change
    @Test
    void flagsPackageSpikeWithNoStructuralChange() {
        AggregatedSignals prior = new AggregatedSignals(2, 10, 2, 6, 3, 1, 60);
        AggregatedSignals current = new AggregatedSignals(2, 11, 2, 7, 3, 1, 60);
        // Simulate 6 new high-tier packages (passed as param)
        AntiGamingEvaluator.Result result = evaluator.evaluate(prior, current, 6, 2);

        assertThat(result.fired()).isTrue();
        assertThat(result.reason()).isEqualTo("HIGH_TIER_PACKAGES_NO_STRUCTURAL_CHANGE");
    }

    // Heuristic 2: confidence jumped HIGH with very low recent commits
    @Test
    void flagsScoreJumpWithoutActivity() {
        // Prior was LOW confidence (totalCommits=1, everything else low)
        AggregatedSignals prior = new AggregatedSignals(0, 1, 1, 1, 1, 0, 30);
        // Current is "HIGH" signals but still only 1 commit in last 30d
        AggregatedSignals current = new AggregatedSignals(1, 25, 4, 12, 5, 4, 90);

        // recentCommits30d = 1 (passed as param)
        AntiGamingEvaluator.Result result = evaluator.evaluate(prior, current, 0, 1);

        assertThat(result.fired()).isTrue();
        assertThat(result.reason()).isEqualTo("SCORE_JUMP_WITHOUT_ACTIVITY");
    }

    // Heuristic 3: consistency collapsed >40 points
    @Test
    void flagsConsistencyCollapse() {
        AggregatedSignals prior = new AggregatedSignals(5, 20, 3, 8, 4, 2, 85);
        AggregatedSignals current = new AggregatedSignals(6, 22, 3, 9, 4, 2, 40);

        AntiGamingEvaluator.Result result = evaluator.evaluate(prior, current, 0, 10);

        assertThat(result.fired()).isTrue();
        assertThat(result.reason()).isEqualTo("CONSISTENCY_COLLAPSE");
    }

    // Heuristic 4: new repo with mature claims
    @Test
    void flagsNewRepoWithMatureClaims() {
        AggregatedSignals prior = new AggregatedSignals(0, 0, 0, 0, 0, 0, 0);
        // repoAgeMonths=0, high services, many packages
        AggregatedSignals current = new AggregatedSignals(5, 5, 1, 0, 4, 4, 80);

        AntiGamingEvaluator.Result result = evaluator.evaluate(prior, current, 8, 5);

        assertThat(result.fired()).isTrue();
        assertThat(result.reason()).isEqualTo("NEW_REPO_MATURE_CLAIMS");
    }

    @Test
    void doesNotFlagHealthyIncrementalGrowth() {
        AggregatedSignals prior = new AggregatedSignals(4, 10, 2, 6, 3, 2, 65);
        AggregatedSignals current = new AggregatedSignals(6, 18, 3, 7, 4, 3, 70);

        AntiGamingEvaluator.Result result = evaluator.evaluate(prior, current, 1, 8);

        assertThat(result.fired()).isFalse();
    }
}
