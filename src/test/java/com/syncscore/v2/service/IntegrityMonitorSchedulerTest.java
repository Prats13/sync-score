package com.syncscore.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.syncscore.v1.domain.AgencyProfile;
import com.syncscore.v1.repo.AgencyProfileRepository;
import com.syncscore.v2.domain.ArchConfidence;
import com.syncscore.v2.domain.ArchStatus;
import com.syncscore.v2.domain.ArchitectureScan;
import com.syncscore.v2.repo.ArchitectureScanRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IntegrityMonitorSchedulerTest {

    @Mock AgencyProfileRepository agencyRepo;
    @Mock ArchitectureScanRepository archScanRepo;
    @Mock V2ScanOrchestrator orchestrator;
    @Mock V2ScanAsyncBridge asyncBridge;

    IntegrityMonitorScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new IntegrityMonitorScheduler(agencyRepo, archScanRepo, orchestrator, asyncBridge);
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "freshnessThresholdDays", 14);
    }

    private AgencyProfile agency(String githubUsername) {
        UUID userId = UUID.randomUUID();
        AgencyProfile a = new AgencyProfile(userId, "Test Agency");
        a.updateProfile("Test Agency", null, null, null, null, githubUsername, false);
        ReflectionTestUtils.setField(a, "id", UUID.randomUUID());
        return a;
    }

    private ArchitectureScan succeededScan(UUID agencyId, Instant finishedAt) {
        ArchitectureScan scan = new ArchitectureScan(agencyId, null, "v2-test");
        ReflectionTestUtils.setField(scan, "id", UUID.randomUUID());
        scan.markRunning();
        scan.markSucceeded(ArchConfidence.MEDIUM, ArchStatus.VERIFIED);
        ReflectionTestUtils.setField(scan, "finishedAt", finishedAt);
        return scan;
    }

    @Test
    void disabled_doesNothing() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        scheduler.runFreshnessCheck();

        verify(agencyRepo, never()).findAllByGithubUsernameIsNotNull();
    }

    @Test
    void noSucceededScan_skipsAgency() {
        AgencyProfile a = agency("octocat");
        when(agencyRepo.findAllByGithubUsernameIsNotNull()).thenReturn(List.of(a));
        when(archScanRepo.findTopByAgencyIdAndStatusOrderByCreatedAtDesc(a.getId(), "SUCCEEDED"))
                .thenReturn(Optional.empty());

        scheduler.runFreshnessCheck();

        verify(orchestrator, never()).markFreshnessLow(any());
        verify(asyncBridge, never()).runAsync(any(), any(), anyInt(), anyInt());
    }

    @Test
    void freshScan_withinThreshold_skips() {
        AgencyProfile a = agency("octocat");
        ArchitectureScan fresh = succeededScan(a.getId(), Instant.now().minus(3, ChronoUnit.DAYS));
        when(agencyRepo.findAllByGithubUsernameIsNotNull()).thenReturn(List.of(a));
        when(archScanRepo.findTopByAgencyIdAndStatusOrderByCreatedAtDesc(a.getId(), "SUCCEEDED"))
                .thenReturn(Optional.of(fresh));

        scheduler.runFreshnessCheck();

        verify(orchestrator, never()).markFreshnessLow(any());
    }

    @Test
    void inFlightScan_skips() {
        AgencyProfile a = agency("octocat");
        ArchitectureScan stale = succeededScan(a.getId(), Instant.now().minus(20, ChronoUnit.DAYS));
        ArchitectureScan running = new ArchitectureScan(a.getId(), null, "v2-test");
        ReflectionTestUtils.setField(running, "id", UUID.randomUUID());
        running.markRunning();

        when(agencyRepo.findAllByGithubUsernameIsNotNull()).thenReturn(List.of(a));
        when(archScanRepo.findTopByAgencyIdAndStatusOrderByCreatedAtDesc(a.getId(), "SUCCEEDED"))
                .thenReturn(Optional.of(stale));
        when(archScanRepo.findTopByAgencyIdOrderByCreatedAtDesc(a.getId()))
                .thenReturn(Optional.of(running));

        scheduler.runFreshnessCheck();

        verify(orchestrator, never()).markFreshnessLow(any());
    }

    @Test
    void staleScan_marksLowAndTriggersNewScan() {
        AgencyProfile a = agency("octocat");
        ArchitectureScan stale = succeededScan(a.getId(), Instant.now().minus(20, ChronoUnit.DAYS));
        UUID newScanId = UUID.randomUUID();

        when(agencyRepo.findAllByGithubUsernameIsNotNull()).thenReturn(List.of(a));
        when(archScanRepo.findTopByAgencyIdAndStatusOrderByCreatedAtDesc(a.getId(), "SUCCEEDED"))
                .thenReturn(Optional.of(stale));
        when(archScanRepo.findTopByAgencyIdOrderByCreatedAtDesc(a.getId()))
                .thenReturn(Optional.of(stale));
        when(orchestrator.createQueuedScan(eq(a.getId()), any())).thenReturn(newScanId);

        scheduler.runFreshnessCheck();

        verify(orchestrator).markFreshnessLow(stale.getId());
        verify(orchestrator).createQueuedScan(eq(a.getId()), any());
        verify(asyncBridge).runAsync(eq(newScanId), eq(a.getId()), eq(0), eq(0));
    }

    @Test
    void multipleAgencies_processesEachIndependently() {
        AgencyProfile fresh = agency("octocat-fresh");
        AgencyProfile stale = agency("octocat-stale");
        UUID newScanId = UUID.randomUUID();

        ArchitectureScan freshScan = succeededScan(fresh.getId(), Instant.now().minus(2, ChronoUnit.DAYS));
        ArchitectureScan staleScan = succeededScan(stale.getId(), Instant.now().minus(30, ChronoUnit.DAYS));

        when(agencyRepo.findAllByGithubUsernameIsNotNull()).thenReturn(List.of(fresh, stale));
        when(archScanRepo.findTopByAgencyIdAndStatusOrderByCreatedAtDesc(fresh.getId(), "SUCCEEDED"))
                .thenReturn(Optional.of(freshScan));
        when(archScanRepo.findTopByAgencyIdAndStatusOrderByCreatedAtDesc(stale.getId(), "SUCCEEDED"))
                .thenReturn(Optional.of(staleScan));
        when(archScanRepo.findTopByAgencyIdOrderByCreatedAtDesc(stale.getId()))
                .thenReturn(Optional.of(staleScan));
        when(orchestrator.createQueuedScan(eq(stale.getId()), any())).thenReturn(newScanId);

        scheduler.runFreshnessCheck();

        verify(orchestrator, never()).markFreshnessLow(freshScan.getId());
        verify(orchestrator).markFreshnessLow(staleScan.getId());
        verify(asyncBridge).runAsync(eq(newScanId), eq(stale.getId()), eq(0), eq(0));
    }
}
