package com.syncscore.v2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.syncscore.v2.domain.ArchConfidence;
import com.syncscore.v2.domain.ArchStatus;
import com.syncscore.v2.domain.ArchitectureReviewCase;
import com.syncscore.v2.domain.ArchitectureScan;
import com.syncscore.v2.repo.ArchitectureReviewCaseRepository;
import com.syncscore.v2.repo.ArchitectureScanRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class V2ReviewCaseServiceTest {

    @Mock ArchitectureReviewCaseRepository reviewCaseRepo;
    @Mock ArchitectureScanRepository archScanRepo;

    V2ReviewCaseService service;

    @BeforeEach
    void setUp() {
        service = new V2ReviewCaseService(reviewCaseRepo, archScanRepo);
    }

    private ArchitectureReviewCase openCase(UUID agencyId, UUID archScanId) {
        ArchitectureReviewCase c = new ArchitectureReviewCase(
                agencyId, archScanId, "HIGH_TIER_PACKAGES_NO_STRUCTURAL_CHANGE", null);
        ReflectionTestUtils.setField(c, "id", UUID.randomUUID());
        return c;
    }

    private ArchitectureScan succeededScan(UUID agencyId) {
        ArchitectureScan scan = new ArchitectureScan(agencyId, null, "v2-test");
        ReflectionTestUtils.setField(scan, "id", UUID.randomUUID());
        scan.markRunning();
        scan.markSucceeded(ArchConfidence.HIGH, ArchStatus.UNDER_REVIEW);
        return scan;
    }

    @Test
    void approve_resolvesCase() {
        UUID agencyId = UUID.randomUUID();
        UUID archScanId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        ArchitectureReviewCase rc = openCase(agencyId, archScanId);
        ArchitectureScan scan = succeededScan(agencyId);

        when(reviewCaseRepo.findById(rc.getId())).thenReturn(Optional.of(rc));
        when(reviewCaseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(archScanRepo.findById(archScanId)).thenReturn(Optional.of(scan));
        when(archScanRepo.save(any())).thenReturn(scan);

        ArchitectureReviewCase result = service.approve(rc.getId(), adminId, "Looks legit");

        assertThat(result.getStatus()).isEqualTo("RESOLVED");
        assertThat(result.getResolvedBy()).isEqualTo(adminId);
        assertThat(result.getResolutionNote()).isEqualTo("Looks legit");
    }

    @Test
    void approve_promotesArchScanToVerified() {
        UUID agencyId = UUID.randomUUID();
        UUID archScanId = UUID.randomUUID();
        ArchitectureReviewCase rc = openCase(agencyId, archScanId);
        ArchitectureScan scan = succeededScan(agencyId);
        ReflectionTestUtils.setField(scan, "id", archScanId);

        when(reviewCaseRepo.findById(rc.getId())).thenReturn(Optional.of(rc));
        when(reviewCaseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(archScanRepo.findById(archScanId)).thenReturn(Optional.of(scan));
        when(archScanRepo.save(any())).thenReturn(scan);

        service.approve(rc.getId(), UUID.randomUUID(), null);

        ArgumentCaptor<ArchitectureScan> captor = ArgumentCaptor.forClass(ArchitectureScan.class);
        verify(archScanRepo).save(captor.capture());
        assertThat(captor.getValue().getArchStatus()).isEqualTo(ArchStatus.VERIFIED);
    }

    @Test
    void dismiss_dismissesCase() {
        UUID agencyId = UUID.randomUUID();
        UUID archScanId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        ArchitectureReviewCase rc = openCase(agencyId, archScanId);

        when(reviewCaseRepo.findById(rc.getId())).thenReturn(Optional.of(rc));
        when(reviewCaseRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArchitectureReviewCase result = service.dismiss(rc.getId(), adminId, "False positive");

        assertThat(result.getStatus()).isEqualTo("DISMISSED");
        assertThat(result.getResolvedBy()).isEqualTo(adminId);
    }

    @Test
    void getCase_throwsNotFoundForUnknownId() {
        UUID unknownId = UUID.randomUUID();
        when(reviewCaseRepo.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCase(unknownId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Review case not found");
    }
}
