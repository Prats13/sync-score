package com.syncscore.v1.scoring.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;

public final class SyncScoreRulesetLoader {
    private static final String DEFAULT_RULESET_PATH = "rulesets/syncscore_v1.json";

    private final ObjectMapper objectMapper;

    public SyncScoreRulesetLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LoadedRuleset loadDefault() {
        return loadFromClasspath(DEFAULT_RULESET_PATH);
    }

    public LoadedRuleset loadFromClasspath(String classpathPath) {
        SyncScoreRuleset ruleset;
        try (InputStream in = new ClassPathResource(classpathPath).getInputStream()) {
            ruleset = objectMapper.readValue(in, SyncScoreRuleset.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load ruleset from classpath: " + classpathPath, e);
        }

        Map<String, CategoryIndexEntry> pkgIndex = new HashMap<>();
        for (SyncScoreRuleset.CategoryRule category : ruleset.categories()) {
            for (String rawPkg : category.packages()) {
                String pkg = rawPkg.toLowerCase(Locale.ROOT);
                CategoryIndexEntry prev = pkgIndex.putIfAbsent(pkg, new CategoryIndexEntry(category.name(), category.pointsEach(), category.cap()));
                if (prev != null) {
                    throw new IllegalStateException("Ruleset has duplicate package across categories: " + pkg);
                }
            }
        }
        return new LoadedRuleset(ruleset, pkgIndex);
    }

    public record LoadedRuleset(
            SyncScoreRuleset ruleset,
            Map<String, CategoryIndexEntry> packageIndex
    ) {}

    public record CategoryIndexEntry(
            String categoryName,
            int pointsEach,
            int categoryCap
    ) {}
}

