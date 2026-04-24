package com.syncscore.v2.api;

import com.syncscore.security.AccessPrincipal;
import com.syncscore.v1.repo.AgencyProfileRepository;
import com.syncscore.v2.api.dto.ArchitectureScanResponse;
import com.syncscore.v2.api.dto.ConfidentialScanRequest;
import com.syncscore.v2.api.dto.ScanDetailResponse;
import com.syncscore.v2.api.dto.ScanRepoResponse;
import com.syncscore.v2.api.dto.ScanSignalResponse;
import com.syncscore.v2.domain.ArchitectureScan;
import com.syncscore.v2.repo.ArchScanRepoRepository;
import com.syncscore.v2.repo.ArchScanStructuralSignalRepository;
import com.syncscore.v2.repo.ArchitectureScanRepository;
import com.syncscore.v2.service.V2ScanAsyncBridge;
import com.syncscore.v2.service.V2ScanOrchestrator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v2/scans")
public class V2ScanController {

    private final V2ScanOrchestrator orchestrator;
    private final V2ScanAsyncBridge asyncBridge;
    private final ArchitectureScanRepository archScanRepo;
    private final ArchScanStructuralSignalRepository signalRepo;
    private final ArchScanRepoRepository scanRepoRepo;
    private final AgencyProfileRepository agencyRepo;

    public V2ScanController(
            V2ScanOrchestrator orchestrator,
            V2ScanAsyncBridge asyncBridge,
            ArchitectureScanRepository archScanRepo,
            ArchScanStructuralSignalRepository signalRepo,
            ArchScanRepoRepository scanRepoRepo,
            AgencyProfileRepository agencyRepo) {
        this.orchestrator = orchestrator;
        this.asyncBridge = asyncBridge;
        this.archScanRepo = archScanRepo;
        this.signalRepo = signalRepo;
        this.scanRepoRepo = scanRepoRepo;
        this.agencyRepo = agencyRepo;
    }

    @GetMapping
    public List<ArchitectureScanResponse> listScans(
            @AuthenticationPrincipal AccessPrincipal principal) {
        var agency = agencyRepo.findByUserId(principal.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agency profile not found"));
        return archScanRepo.findByAgencyIdOrderByCreatedAtDesc(agency.getId())
                .stream().map(this::toResponse).toList();
    }

    @PostMapping("/trigger")
    public ArchitectureScanResponse triggerScan(
            @AuthenticationPrincipal AccessPrincipal principal,
            @RequestBody(required = false) ConfidentialScanRequest request) {
        var agency = agencyRepo.findByUserId(principal.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agency profile not found"));

        UUID archScanId = (request != null && StringUtils.hasText(request.source()))
                ? orchestrator.createConfidentialScan(agency.getId(), request)
                : orchestrator.createQueuedScan(agency.getId(), null);

        asyncBridge.runAsync(archScanId, agency.getId(), 0, 0);

        ArchitectureScan scan = archScanRepo.findById(archScanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Scan creation failed"));
        return toResponse(scan);
    }

    @GetMapping("/{scanId}")
    public ArchitectureScanResponse getScan(
            @AuthenticationPrincipal AccessPrincipal principal,
            @PathVariable UUID scanId) {
        ArchitectureScan scan = authorizeScan(scanId, principal);
        return toResponse(scan);
    }

    @GetMapping("/{scanId}/detail")
    public ScanDetailResponse getScanDetail(
            @AuthenticationPrincipal AccessPrincipal principal,
            @PathVariable UUID scanId) {
        ArchitectureScan scan = authorizeScan(scanId, principal);

        List<ScanSignalResponse> signals = signalRepo.findByArchitectureScanId(scanId).stream()
                .map(s -> new ScanSignalResponse(
                        s.getSignalType().name(),
                        s.getValueNumeric(),
                        s.getValueLabel(),
                        s.getConfidenceContribution()))
                .toList();

        List<ScanRepoResponse> repos = scanRepoRepo.findByArchitectureScanId(scanId).stream()
                .map(r -> new ScanRepoResponse(
                        r.getRepoFullName(),
                        r.getCommits30d(),
                        r.getCommits90d(),
                        r.getContributorCount(),
                        r.getMaxFolderDepth(),
                        r.getServiceCount(),
                        r.getSourceFileCount(),
                        r.getRepoAgeMonths()))
                .toList();

        return new ScanDetailResponse(toResponse(scan), signals, repos);
    }

    private ArchitectureScan authorizeScan(UUID scanId, AccessPrincipal principal) {
        ArchitectureScan scan = archScanRepo.findById(scanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Architecture scan not found"));
        var agency = agencyRepo.findById(scan.getAgencyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agency not found"));
        if (!agency.getUserId().equals(principal.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your scan");
        }
        return scan;
    }

    private ArchitectureScanResponse toResponse(ArchitectureScan scan) {
        return new ArchitectureScanResponse(
                scan.getId(),
                scan.getAgencyId(),
                scan.getStatus(),
                scan.getConfidence(),
                scan.getArchStatus(),
                scan.getEvidenceSource(),
                scan.getRulesetVersion(),
                scan.getLlmScore(),
                scan.getLlmReasoning(),
                scan.getCreatedAt(),
                scan.getFinishedAt()
        );
    }
}
