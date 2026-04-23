package com.syncscore.v2.service;

import java.util.UUID;

public record ScanCompletedEvent(UUID scanEventId, UUID agencyId, int detectedPackageCount, int newHighTierPackageCount) {}
