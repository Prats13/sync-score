package com.syncscore.v1.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final MeterRegistry meterRegistry;
    private final Timer durationTimer;
    private final Counter errorCounter;

    public V1ScanAsyncBridge(V1ScanOrchestrator orchestrator, MeterRegistry meterRegistry) {
        this.orchestrator = orchestrator;
        this.meterRegistry = meterRegistry;
        this.durationTimer = meterRegistry.timer("syncscore.v1.scan.duration");
        this.errorCounter = meterRegistry.counter("syncscore.v1.scan.errors");
    }

    @Async("v1ScannerExecutor")
    public CompletableFuture<Void> runAsync(UUID scanEventId) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            orchestrator.runScan(scanEventId);
        } catch (Exception e) {
            errorCounter.increment();
            // runScan's transaction already rolled back at this point; record
            // failure in a fresh REQUIRES_NEW transaction so no partial state survives.
            orchestrator.markScanFailed(
                    scanEventId,
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
            );
        } finally {
            sample.stop(durationTimer);
        }
        return CompletableFuture.completedFuture(null);
    }
}
