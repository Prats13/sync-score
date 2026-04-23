package com.syncscore.v2.api;

import com.syncscore.v2.api.dto.ArchitectureProfileResponse;
import com.syncscore.v2.domain.ArchitectureScan;
import com.syncscore.v2.repo.ArchitectureReviewCaseRepository;
import com.syncscore.v2.repo.ArchitectureScanRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v2/profile")
public class V2ProfileController {

    private final ArchitectureScanRepository archScanRepo;
    private final ArchitectureReviewCaseRepository reviewCaseRepo;

    public V2ProfileController(
            ArchitectureScanRepository archScanRepo,
            ArchitectureReviewCaseRepository reviewCaseRepo) {
        this.archScanRepo = archScanRepo;
        this.reviewCaseRepo = reviewCaseRepo;
    }

    @GetMapping("/{agencyId}/architecture")
    public ArchitectureProfileResponse getArchitecture(@PathVariable UUID agencyId) {
        ArchitectureScan scan = archScanRepo.findTopByAgencyIdAndStatusOrderByCreatedAtDesc(agencyId, "SUCCEEDED")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No completed architecture scan for this agency"));

        boolean hasOpenCase = reviewCaseRepo
                .findTopByAgencyIdAndStatusOrderByCreatedAtDesc(agencyId, "OPEN")
                .isPresent();

        return new ArchitectureProfileResponse(
                agencyId,
                scan.getConfidence(),
                scan.getArchStatus(),
                scan.getEvidenceSource(),
                hasOpenCase,
                scan.getFinishedAt()
        );
    }
}

