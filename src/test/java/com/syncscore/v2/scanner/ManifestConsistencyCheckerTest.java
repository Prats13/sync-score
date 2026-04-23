package com.syncscore.v2.scanner;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManifestConsistencyCheckerTest {

    private final ManifestConsistencyChecker checker = new ManifestConsistencyChecker();

    @Test
    void highConsistencyWhenSourceFilesExceedPackages() {
        // 20 source files, 5 detected packages → HIGH
        int score = checker.computeConsistencyScore(20, 5);
        assertThat(score).isGreaterThanOrEqualTo(80);
    }

    @Test
    void lowConsistencyWhenNoSourceFilesButPackagesExist() {
        // 0 source files, 8 detected packages → 0
        int score = checker.computeConsistencyScore(0, 8);
        assertThat(score).isEqualTo(0);
    }

    @Test
    void fullScoreWhenNoPackagesDeclared() {
        // nothing claimed, nothing to check
        int score = checker.computeConsistencyScore(0, 0);
        assertThat(score).isEqualTo(100);
    }

    @Test
    void mediumConsistencyForBalancedRatio() {
        // 3 source files, 5 detected packages
        int score = checker.computeConsistencyScore(3, 5);
        assertThat(score).isGreaterThan(0).isLessThan(80);
    }
}