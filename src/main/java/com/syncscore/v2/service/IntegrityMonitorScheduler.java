package com.syncscore.v2.service;

import com.syncscore.v1.domain.AgencyProfile;
import com.syncscore.v1.repo.AgencyProfileRepository;
import com.syncscore.v2.domain.ArchitectureScan;
import com.syncscore.v2.repo.ArchitectureScanRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class IntegrityMonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(IntegrityMonitorScheduler.class);

    private final AgencyProfileRepository agencyRepo;
    private final ArchitectureScanRepository archScanRepo;
    private final V2ScanOrchestrator orchestrator;
    private final V2ScanAsyncBridge asyncBridge;

    @Value("${app.integrity.enabled:true}")
    private boolean enabled;

    @Value("${app.integrity.freshness-threshold-days:14}")
    private int freshnessThresholdDays;

    public IntegrityMonitorScheduler(
            AgencyProfileRepository agencyRepo,
            ArchitectureScanRepository archScanRepo,
            V2ScanOrchestrator orchestrator,
            V2ScanAsyncBridge asyncBridge) {
        this.agencyRepo = agencyRepo;
        this.archScanRepo = archScanRepo;
        this.orchestrator = orchestrator;
        this.asyncBridge = asyncBridge;
    }

    @Scheduled(cron = "${app.integrity.cron:0 0 2 * * *}")
    public void runFreshnessCheck() {
        if (!enabled) return;

        Instant threshold = Instant.now().minus(freshnessThresholdDays, ChronoUnit.DAYS);
        List<AgencyProfile> candidates = agencyRepo.findAllByGithubUsernameIsNotNull();
        log.info("event=INTEGRITY_CHECK_STARTED candidates={} freshnessThresholdDays={}", candidates.size(), freshnessThresholdDays);

        int triggered = 0;
        for (AgencyProfile agency : candidates) {
            try {
                triggered += processAgency(agency, threshold);
            } catch (Exception e) {
                log.error("event=INTEGRITY_CHECK_ERROR agencyId={} error={}", agency.getId(), e.getMessage());
            }
        }

        log.info("event=INTEGRITY_CHECK_FINISHED candidates={} triggered={}", candidates.size(), triggered);
    }

    private int processAgency(AgencyProfile agency, Instant threshold) {
        UUID agencyId = agency.getId();

        Optional<ArchitectureScan> latestSucceeded = archScanRepo
                .findTopByAgencyIdAndStatusOrderByCreatedAtDesc(agencyId, "SUCCEEDED");
        if (latestSucceeded.isEmpty()) return 0;

        ArchitectureScan stale = latestSucceeded.get();
        if (stale.getFinishedAt() == null || stale.getFinishedAt().isAfter(threshold)) return 0;

        // Skip if a scan is already in flight
        Optional<ArchitectureScan> latest = archScanRepo.findTopByAgencyIdOrderByCreatedAtDesc(agencyId);
        if (latest.isPresent()) {
            String status = latest.get().getStatus();
            if ("QUEUED".equals(status) || "RUNNING".equals(status)) return 0;
        }

        log.warn("event=FRESHNESS_DETECTED agencyId={} archScanId={} finishedAt={}",
                agencyId, stale.getId(), stale.getFinishedAt());

        orchestrator.markFreshnessLow(stale.getId());

        UUID newArchScanId = orchestrator.createQueuedScan(agencyId, stale.getScanEventId());
        asyncBridge.runAsync(newArchScanId, agencyId, 0, 0);

        log.info("event=INTEGRITY_SCAN_TRIGGERED agencyId={} archScanId={}", agencyId, newArchScanId);
        return 1;
    }
}
