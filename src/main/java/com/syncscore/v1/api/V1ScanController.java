package com.syncscore.v1.api;

import com.syncscore.security.AccessPrincipal;
import com.syncscore.v1.api.dto.ScanStatusResponse;
import com.syncscore.v1.domain.AgencyProfile;
import com.syncscore.v1.domain.DetectedPackage;
import com.syncscore.v1.domain.RepositoryScan;
import com.syncscore.v1.domain.ScanEvent;
import com.syncscore.v1.domain.ScoreResult;
import com.syncscore.v1.repo.AgencyProfileRepository;
import com.syncscore.v1.repo.DetectedPackageRepository;
import com.syncscore.v1.repo.RepositoryScanRepository;
import com.syncscore.v1.repo.ScanEventRepository;
import com.syncscore.v1.repo.ScoreResultRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/scans")
public class V1ScanController {

    private final ScanEventRepository scanEventRepo;
    private final AgencyProfileRepository agencyRepo;
    private final RepositoryScanRepository repoScanRepo;
    private final ScoreResultRepository scoreRepo;
    private final DetectedPackageRepository detectedRepo;

    public V1ScanController(
            ScanEventRepository scanEventRepo,
            AgencyProfileRepository agencyRepo,
            RepositoryScanRepository repoScanRepo,
            ScoreResultRepository scoreRepo,
            DetectedPackageRepository detectedRepo
    ) {
        this.scanEventRepo = scanEventRepo;
        this.agencyRepo = agencyRepo;
        this.repoScanRepo = repoScanRepo;
        this.scoreRepo = scoreRepo;
        this.detectedRepo = detectedRepo;
    }

    @GetMapping("/{scanId}")
    public ScanStatusResponse getScan(
            @AuthenticationPrincipal AccessPrincipal principal,
            @PathVariable UUID scanId
    ) {
        ScanEvent ev = scanEventRepo.findById(scanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Scan not found"));

        AgencyProfile agency = agencyRepo.findById(ev.getAgencyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agency not found"));
        if (!agency.getUserId().equals(principal.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your scan");
        }

        List<RepositoryScan> repos = repoScanRepo.findByScanEventId(ev.getId());
        List<DetectedPackage> detected = detectedRepo.findByScanEventId(ev.getId());
        Optional<ScoreResult> score = scoreRepo.findByScanEventId(ev.getId());

        ScanStatusResponse.Score scoreResp = score.map(s -> new ScanStatusResponse.Score(
                s.getTotalScore(),
                s.getTier(),
                s.getCategorySubtotals()
        )).orElse(null);

        List<ScanStatusResponse.Repository> repoResp = repos.stream()
                .map(r -> new ScanStatusResponse.Repository(
                        r.getId(),
                        r.getRepoFullName(),
                        r.getRepoUrl(),
                        r.getDefaultBranch(),
                        r.getStatus(),
                        r.getManifestsFound()
                ))
                .toList();

        List<ScanStatusResponse.Detected> detResp = detected.stream()
                .map(d -> new ScanStatusResponse.Detected(
                        d.getPackageNameNormalized(),
                        d.getCategory(),
                        d.getPointsAwarded(),
                        d.getManifestType(),
                        d.getManifestPath(),
                        d.getRepositoryScanId()
                ))
                .toList();

        return new ScanStatusResponse(
                ev.getId(),
                ev.getAgencyId(),
                ev.getTriggerType(),
                ev.getStatus(),
                ev.getErrorMessage(),
                ev.getRulesetVersion(),
                ev.getVerificationLabel(),
                ev.getEvidenceItemIds(),
                ev.getStartedAt(),
                ev.getFinishedAt(),
                ev.getCreatedAt(),
                scoreResp,
                repoResp,
                detResp
        );
    }
}

