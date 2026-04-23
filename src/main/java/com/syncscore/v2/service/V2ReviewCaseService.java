package com.syncscore.v2.service;

import com.syncscore.v2.domain.ArchStatus;
import com.syncscore.v2.domain.ArchitectureReviewCase;
import com.syncscore.v2.domain.ArchitectureScan;
import com.syncscore.v2.repo.ArchitectureReviewCaseRepository;
import com.syncscore.v2.repo.ArchitectureScanRepository;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class V2ReviewCaseService {

    private static final Logger log = LoggerFactory.getLogger(V2ReviewCaseService.class);

    private final ArchitectureReviewCaseRepository reviewCaseRepo;
    private final ArchitectureScanRepository archScanRepo;

    public V2ReviewCaseService(ArchitectureReviewCaseRepository reviewCaseRepo,
                               ArchitectureScanRepository archScanRepo) {
        this.reviewCaseRepo = reviewCaseRepo;
        this.archScanRepo = archScanRepo;
    }

    public List<ArchitectureReviewCase> listOpen() {
        return reviewCaseRepo.findByStatusOrderByCreatedAtDesc("OPEN");
    }

    public ArchitectureReviewCase getCase(UUID caseId) {
        return reviewCaseRepo.findById(caseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Review case not found"));
    }

    @Transactional
    public ArchitectureReviewCase approve(UUID caseId, UUID adminUserId, String note) {
        ArchitectureReviewCase reviewCase = getCase(caseId);
        reviewCase.resolve(adminUserId, note);
        reviewCaseRepo.save(reviewCase);
        log.info("event=REVIEW_CASE_RESOLVED caseId={} agencyId={} adminUserId={}",
                reviewCase.getId(), reviewCase.getAgencyId(), adminUserId);

        archScanRepo.findById(reviewCase.getArchitectureScanId()).ifPresent(scan -> {
            scan.markSucceeded(scan.getConfidence(), ArchStatus.VERIFIED);
            archScanRepo.save(scan);
        });
        return reviewCase;
    }

    @Transactional
    public ArchitectureReviewCase dismiss(UUID caseId, UUID adminUserId, String note) {
        ArchitectureReviewCase reviewCase = getCase(caseId);
        reviewCase.dismiss(adminUserId, note);
        reviewCaseRepo.save(reviewCase);
        log.info("event=REVIEW_CASE_DISMISSED caseId={} agencyId={} adminUserId={}",
                reviewCase.getId(), reviewCase.getAgencyId(), adminUserId);
        return reviewCase;
    }
}
