package com.syncscore.v1.service;

import com.syncscore.v1.domain.ScanTriggerType;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class V1ScanRunner {

    private final V1ScanOrchestrator orchestrator;
    private final V1ScanAsyncBridge asyncBridge;

    public V1ScanRunner(V1ScanOrchestrator orchestrator, V1ScanAsyncBridge asyncBridge) {
        this.orchestrator = orchestrator;
        this.asyncBridge = asyncBridge;
    }

    public UUID enqueueInitialScan(UUID agencyId) {
        return enqueueScan(agencyId, ScanTriggerType.INITIAL);
    }

    public UUID enqueueRescan(UUID agencyId) {
        return enqueueScan(agencyId, ScanTriggerType.RESCAN);
    }

    public UUID enqueueScan(UUID agencyId, ScanTriggerType triggerType) {
        UUID scanEventId = orchestrator.createQueuedScan(agencyId, triggerType);
        asyncBridge.runAsync(scanEventId);
        return scanEventId;
    }
}
