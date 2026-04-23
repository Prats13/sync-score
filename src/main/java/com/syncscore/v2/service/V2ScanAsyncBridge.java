package com.syncscore.v2.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class V2ScanAsyncBridge {

    private final V2ScanOrchestrator orchestrator;
    private final MeterRegistry meterRegistry;
    private final Timer durationTimer;
    private final Counter errorCounter;

    public V2ScanAsyncBridge(V2ScanOrchestrator orchestrator, MeterRegistry meterRegistry) {
        this.orchestrator = orchestrator;
        this.meterRegistry = meterRegistry;
        this.durationTimer = meterRegistry.timer("syncscore.v2.arch_scan.duration");
        this.errorCounter = meterRegistry.counter("syncscore.v2.arch_scan.errors");
    }

    @Async("v2ScannerExecutor")
    public CompletableFuture<Void> runAsync(UUID archScanId, UUID agencyId, int detectedPackageCount, int newHighTierPackages) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            orchestrator.runScan(archScanId, agencyId, detectedPackageCount, newHighTierPackages);
        } catch (Exception e) {
            errorCounter.increment();
            orchestrator.markScanFailed(archScanId,
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        } finally {
            sample.stop(durationTimer);
        }
        return CompletableFuture.completedFuture(null);
    }
}
