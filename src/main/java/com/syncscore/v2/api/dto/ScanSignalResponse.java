package com.syncscore.v2.api.dto;

import java.math.BigDecimal;

public record ScanSignalResponse(
        String signalType,
        BigDecimal valueNumeric,
        String valueLabel,
        BigDecimal confidenceContribution
) {}