package com.syncscore.v1.scoring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncscore.v1.scoring.rules.SyncScoreRulesetLoader;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SyncScoreScorerTest {

    @Test
    void scoresWithCategoryCapsAndTiers() {
        SyncScoreRulesetLoader loader = new SyncScoreRulesetLoader(new ObjectMapper());
        SyncScoreScorer scorer = new SyncScoreScorer(loader.loadDefault());

        SyncScoreScorer.SyncScoreResult r = scorer.scorePackages(List.of(
                "langgraph", "crewai", "autogen", // orchestration cap 30 => third gets 0
                "pinecone-client", "weaviate-client", // rag cap 20 => both count
                "openai" // base sdk
        ));

        assertThat(r.categorySubtotals().get("Orchestration")).isEqualTo(30);
        assertThat(r.categorySubtotals().get("RAG & Retrieval")).isEqualTo(20);
        assertThat(r.categorySubtotals().get("Base SDK")).isEqualTo(2);
        assertThat(r.totalScore()).isEqualTo(52);
        assertThat(r.tier()).isEqualTo("Builder");

        // Orchestration cap is 30 with 15 points each => only 2 packages can contribute points.
        var orchestration = r.detectedPackages().stream()
                .filter(p -> p.category().equals("Orchestration"))
                .toList();
        assertThat(orchestration).hasSize(3);
        assertThat(orchestration.stream().filter(p -> p.pointsAwarded() == 0).count()).isEqualTo(1);
    }

    @Test
    void wrapperTierAtLowerScores() {
        SyncScoreRulesetLoader loader = new SyncScoreRulesetLoader(new ObjectMapper());
        SyncScoreScorer scorer = new SyncScoreScorer(loader.loadDefault());

        SyncScoreScorer.SyncScoreResult r = scorer.scorePackages(List.of("openai"));
        assertThat(r.totalScore()).isEqualTo(2);
        assertThat(r.tier()).isEqualTo("Wrapper");
    }

    @Test
    void expertTierAtHighScores() {
        SyncScoreRulesetLoader loader = new SyncScoreRulesetLoader(new ObjectMapper());
        SyncScoreScorer scorer = new SyncScoreScorer(loader.loadDefault());

        // Enough to cross 70 deterministically.
        SyncScoreScorer.SyncScoreResult r = scorer.scorePackages(List.of(
                "langgraph", "crewai", // 30
                "pinecone-client", "weaviate-client", // 20 => 50
                "mem0ai", "redis", // 15 cap => 15 => 65
                "nemoguardrails", "guardrails-ai", // 15 cap => 15 => 80
                "langfuse", "langsmith" // 15 cap => 15 (but already expert anyway)
        ));
        assertThat(r.totalScore()).isGreaterThanOrEqualTo(70);
        assertThat(r.tier()).isEqualTo("Expert");
    }
}
