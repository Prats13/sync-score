package com.syncscore.v2.scoring;

import com.syncscore.v2.domain.ArchConfidence;
import com.syncscore.v2.domain.ArchStatus;
import com.syncscore.v2.scanner.StructuralSignalAggregator.AggregatedSignals;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class V2ConfidenceScorerTest {

    private final V2ConfidenceScorer scorer = new V2ConfidenceScorer();

    @Test
    void highConfidenceForActiveWellStructuredRepo() {
        AggregatedSignals signals = new AggregatedSignals(10, 25, 4, 12, 5, 4, 90);
        V2ConfidenceScorer.Result result = scorer.score(signals, false);
        assertThat(result.confidence()).isEqualTo(ArchConfidence.HIGH);
        assertThat(result.archStatus()).isEqualTo(ArchStatus.VERIFIED);
    }

    @Test
    void lowConfidenceForBareRepo() {
        AggregatedSignals signals = new AggregatedSignals(0, 0, 0, 0, 0, 0, 0);
        V2ConfidenceScorer.Result result = scorer.score(signals, false);
        assertThat(result.confidence()).isEqualTo(ArchConfidence.LOW);
    }

    @Test
    void evidenceMismatchWhenConsistencyBelowThreshold() {
        AggregatedSignals signals = new AggregatedSignals(5, 30, 3, 8, 4, 2, 20);
        V2ConfidenceScorer.Result result = scorer.score(signals, false);
        assertThat(result.archStatus()).isEqualTo(ArchStatus.EVIDENCE_MISMATCH);
    }

    @Test
    void underReviewWhenAntiGamingFired() {
        AggregatedSignals signals = new AggregatedSignals(10, 25, 4, 12, 5, 4, 90);
        V2ConfidenceScorer.Result result = scorer.score(signals, true);
        assertThat(result.archStatus()).isEqualTo(ArchStatus.UNDER_REVIEW);
    }

    @Test
    void mediumConfidenceForModerateSignals() {
        AggregatedSignals signals = new AggregatedSignals(3, 8, 2, 3, 3, 2, 55);
        V2ConfidenceScorer.Result result = scorer.score(signals, false);
        assertThat(result.confidence()).isEqualTo(ArchConfidence.MEDIUM);
    }
}
