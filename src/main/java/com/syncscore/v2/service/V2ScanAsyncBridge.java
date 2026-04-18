package com.syncscore.v2.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class V2ScanAsyncBridge {

    private final V2ScanOrchestrator orchestrator;

    public V2ScanAsyncBridge(V2ScanOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Async("v2ScannerExecutor")
    public CompletableFuture<Void> runAsync(UUID archScanId, UUID agencyId, int detectedPackageCount, int newHighTierPackages) {
        try {
            orchestrator.runScan(archScanId, agencyId, detectedPackageCount, newHighTierPackages);
        } catch (Exception e) {
            orchestrator.markScanFailed(archScanId,
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
        return CompletableFuture.completedFuture(null);
    }
}
