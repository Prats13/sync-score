package com.syncscore.v2.scanner;

import org.springframework.stereotype.Component;

@Component
public class ManifestConsistencyChecker {

    /**
     * Scores consistency between detected packages and visible source files.
     * Returns 0–100: higher means more evidence that the declared packages are actually used.
     */
    public int computeConsistencyScore(int sourceFileCount, int detectedPackageCount) {
        if (detectedPackageCount == 0) {
            return 100; // no claims to check
        }
        if (sourceFileCount == 0) {
            return 0; // packages claimed but zero source code visible
        }
        // Ratio: score = min(100, (sourceFiles / packages) * 40)
        // 2.5x source files per package = 100; 1x = 40; 0.5x = 20
        double ratio = (double) sourceFileCount / detectedPackageCount;
        int score = (int) Math.min(100, ratio * 40);
        return score;
    }
}