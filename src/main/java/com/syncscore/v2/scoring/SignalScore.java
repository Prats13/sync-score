package com.syncscore.v2.scoring;

import com.syncscore.v2.domain.StructuralSignalType;
import java.math.BigDecimal;

public record SignalScore(
        StructuralSignalType signalType,
        BigDecimal rawValue,
        String valueLabel,
        BigDecimal weightedContribution   // 0–100 weighted points
) {}