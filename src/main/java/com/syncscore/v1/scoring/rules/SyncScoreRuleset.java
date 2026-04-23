package com.syncscore.v1.scoring.rules;

import java.util.List;

public record SyncScoreRuleset(
        String rulesetVersion,
        int scoreMin,
        int scoreMax,
        List<TierRule> tiers,
        List<CategoryRule> categories
) {
    public record TierRule(String name, int min, int max) {}

    public record CategoryRule(
            String name,
            int pointsEach,
            int cap,
            List<String> packages
    ) {}
}

