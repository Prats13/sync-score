package com.syncscore.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncscore.v2.api.dto.ConfidentialScanRequest;
import com.syncscore.v2.domain.ArchConfidence;
import com.syncscore.v2.domain.ArchStatus;
import com.syncscore.v2.domain.ArchitectureScan;
import com.syncscore.v2.domain.ConfidentialScanSession;
import com.syncscore.v2.repo.ArchScanStructuralSignalRepository;
import com.syncscore.v2.repo.ArchitectureReviewCaseRepository;
import com.syncscore.v2.repo.ArchitectureScanRepository;
import com.syncscore.v2.repo.ConfidentialScanSessionRepository;
import com.syncscore.v2.scanner.V2StructuralScanner;
import com.syncscore.v2.scoring.AntiGamingEvaluator;
import com.syncscore.v2.scoring.V2ConfidenceScorer;
import com.syncscore.v1.repo.AgencyProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class V2ScanOrchestratorTest {

    @Mock ArchitectureScanRepository archScanRepo;
    @Mock ArchScanStructuralSignalRepository signalRepo;
    @Mock ArchitectureReviewCaseRepository reviewCaseRepo;
    @Mock ConfidentialScanSessionRepository sessionRepo;
    @Mock AgencyProfileRepository agencyRepo;
    @Mock V2StructuralScanner structuralScanner;
    @Mock V2ConfidenceScorer confidenceScorer;
    @Mock AntiGamingEvaluator antiGamingEvaluator;
    @Mock V2ScanAsyncBridge asyncBridge;

    V2ScanOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new V2ScanOrchestrator(
                archScanRepo, signalRepo, reviewCaseRepo, sessionRepo,
                agencyRepo, structuralScanner, confidenceScorer,
                antiGamingEvaluator, asyncBridge, new ObjectMapper());
    }

    private ArchitectureScan stubbedScan(UUID agencyId) {
        ArchitectureScan scan = new ArchitectureScan(agencyId, null, V2ScanOrchestrator.RULESET_VERSION);
        ReflectionTestUtils.setField(scan, "id", UUID.randomUUID());
        return scan;
    }

    @Test
    void createConfidentialScan_githubSource_setsCorrectEvidenceSource() {
        UUID agencyId = UUID.randomUUID();
        ArchitectureScan saved = stubbedScan(agencyId);
        when(archScanRepo.save(any())).thenReturn(saved);
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.createConfidentialScan(agencyId,
                new ConfidentialScanRequest("github", List.of("secrets"), ""));

        ArgumentCaptor<ArchitectureScan> captor = ArgumentCaptor.forClass(ArchitectureScan.class);
        verify(archScanRepo).save(captor.capture());
        assertThat(captor.getValue().getEvidenceSource()).isEqualTo("CONFIDENTIAL_GITHUB");
    }

    @Test
    void createConfidentialScan_interviewSource_setsCorrectEvidenceSource() {
        UUID agencyId = UUID.randomUUID();
        ArchitectureScan saved = stubbedScan(agencyId);
        when(archScanRepo.save(any())).thenReturn(saved);
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.createConfidentialScan(agencyId,
                new ConfidentialScanRequest("interview", List.of(), null));

        ArgumentCaptor<ArchitectureScan> captor = ArgumentCaptor.forClass(ArchitectureScan.class);
        verify(archScanRepo).save(captor.capture());
        assertThat(captor.getValue().getEvidenceSource()).isEqualTo("CONFIDENTIAL_INTERVIEW");
    }

    @Test
    void createConfidentialScan_persistsSessionWithCorrectSourceType() {
        UUID agencyId = UUID.randomUUID();
        ArchitectureScan saved = stubbedScan(agencyId);
        when(archScanRepo.save(any())).thenReturn(saved);
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.createConfidentialScan(agencyId,
                new ConfidentialScanRequest("docs", List.of("pii"), "custom/path"));

        ArgumentCaptor<ConfidentialScanSession> captor = ArgumentCaptor.forClass(ConfidentialScanSession.class);
        verify(sessionRepo).save(captor.capture());
        assertThat(captor.getValue().getSourceType()).isEqualTo("docs");
        assertThat(captor.getValue().getCustomExclusions()).isEqualTo("custom/path");
    }

    @Test
    void markFreshnessLow_updatesArchStatusOnExistingScan() {
        UUID scanId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        ArchitectureScan scan = stubbedScan(agencyId);
        ReflectionTestUtils.setField(scan, "id", scanId);
        scan.markSucceeded(ArchConfidence.HIGH, ArchStatus.VERIFIED);
        when(archScanRepo.findById(scanId)).thenReturn(Optional.of(scan));
        when(archScanRepo.save(any())).thenReturn(scan);

        orchestrator.markFreshnessLow(scanId);

        ArgumentCaptor<ArchitectureScan> captor = ArgumentCaptor.forClass(ArchitectureScan.class);
        verify(archScanRepo).save(captor.capture());
        assertThat(captor.getValue().getArchStatus()).isEqualTo(ArchStatus.FRESHNESS_LOW);
        assertThat(captor.getValue().getConfidence()).isEqualTo(ArchConfidence.HIGH);
    }

    @Test
    void markScanFailed_setsStatusToFailed() {
        UUID scanId = UUID.randomUUID();
        UUID agencyId = UUID.randomUUID();
        ArchitectureScan scan = stubbedScan(agencyId);
        ReflectionTestUtils.setField(scan, "id", scanId);
        when(archScanRepo.findById(scanId)).thenReturn(Optional.of(scan));
        when(archScanRepo.save(any())).thenReturn(scan);

        orchestrator.markScanFailed(scanId, "timeout");

        ArgumentCaptor<ArchitectureScan> captor = ArgumentCaptor.forClass(ArchitectureScan.class);
        verify(archScanRepo).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getErrorMessage()).isEqualTo("timeout");
    }

    @Test
    void createConfidentialScan_unknownSource_fallsBackToMixed() {
        UUID agencyId = UUID.randomUUID();
        ArchitectureScan saved = stubbedScan(agencyId);
        when(archScanRepo.save(any())).thenReturn(saved);
        when(sessionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.createConfidentialScan(agencyId,
                new ConfidentialScanRequest("unknown_type", List.of(), ""));

        ArgumentCaptor<ArchitectureScan> captor = ArgumentCaptor.forClass(ArchitectureScan.class);
        verify(archScanRepo).save(captor.capture());
        assertThat(captor.getValue().getEvidenceSource()).isEqualTo("CONFIDENTIAL_MIXED");
    }
}
