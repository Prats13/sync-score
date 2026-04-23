package com.syncscore.v1.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Thin async wrapper around V1ScanOrchestrator.
 *
 * Exists as a separate Spring bean so that @Async is honoured via the AOP proxy.
 * Self-invocation (calling runAsync from within the same bean) bypasses the proxy,
 * causing @Async to be silently ignored and the scan to block the request thread.
 */
@Service
public class V1ScanAsyncBridge {

    private final V1ScanOrchestrator orchestrator;

    public V1ScanAsyncBridge(V1ScanOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Async("v1ScannerExecutor")
    public CompletableFuture<Void> runAsync(UUID scanEventId) {
        try {
            orchestrator.runScan(scanEventId);
        } catch (Exception e) {
            // runScan's transaction already rolled back at this point; record
            // failure in a fresh REQUIRES_NEW transaction so no partial state survives.
            orchestrator.markScanFailed(
                    scanEventId,
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
            );
        }
        return CompletableFuture.completedFuture(null);
    }
}
