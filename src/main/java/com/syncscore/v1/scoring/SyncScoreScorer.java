package com.syncscore.v1.scoring;

import com.syncscore.v1.scoring.rules.SyncScoreRuleset;
import com.syncscore.v1.scoring.rules.SyncScoreRulesetLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SyncScoreScorer {
    private final SyncScoreRulesetLoader.LoadedRuleset loaded;

    public SyncScoreScorer(SyncScoreRulesetLoader.LoadedRuleset loaded) {
        this.loaded = loaded;
    }

    public SyncScoreResult scorePackages(Collection<String> normalizedPackages) {
        SyncScoreRuleset ruleset = loaded.ruleset();

        Map<String, Integer> subtotals = new LinkedHashMap<>();
        Map<String, Integer> running = new HashMap<>();
        for (SyncScoreRuleset.CategoryRule c : ruleset.categories()) {
            subtotals.put(c.name(), 0);
            running.put(c.name(), 0);
        }

        List<String> pkgs = normalizedPackages == null ? List.of() : normalizedPackages.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();

        List<DetectedPackage> detected = new ArrayList<>();
        for (String pkg : pkgs) {
            SyncScoreRulesetLoader.CategoryIndexEntry entry = loaded.packageIndex().get(pkg);
            if (entry == null) {
                continue;
            }
            int soFar = running.getOrDefault(entry.categoryName(), 0);
            int award = 0;
            if (soFar < entry.categoryCap()) {
                int next = Math.min(soFar + entry.pointsEach(), entry.categoryCap());
                award = next - soFar; // may be partial if cap isn't divisible by pointsEach
                running.put(entry.categoryName(), next);
                subtotals.put(entry.categoryName(), next);
            }
            detected.add(new DetectedPackage(pkg, entry.categoryName(), award));
        }

        int total = subtotals.values().stream().mapToInt(Integer::intValue).sum();
        total = Math.min(total, ruleset.scoreMax());

        String tier = resolveTier(total, ruleset);

        return new SyncScoreResult(
                total,
                tier,
                ruleset.rulesetVersion(),
                subtotals,
                detected
        );
    }

    private static String resolveTier(int score, SyncScoreRuleset ruleset) {
        for (SyncScoreRuleset.TierRule t : ruleset.tiers()) {
            if (score >= t.min() && score <= t.max()) {
                return t.name();
            }
        }
        throw new IllegalStateException("Score " + score + " does not map to a tier");
    }

    public record DetectedPackage(String packageName, String category, int pointsAwarded) {}

    public record SyncScoreResult(
            int totalScore,
            String tier,
            String rulesetVersion,
            Map<String, Integer> categorySubtotals,
            List<DetectedPackage> detectedPackages
    ) {}
}

