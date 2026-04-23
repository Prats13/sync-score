package com.syncscore.v2.api;

import com.syncscore.security.AccessPrincipal;
import com.syncscore.v1.repo.AgencyProfileRepository;
import com.syncscore.v2.api.dto.ArchitectureScanResponse;
import com.syncscore.v2.domain.ArchitectureScan;
import com.syncscore.v2.repo.ArchitectureScanRepository;
import com.syncscore.v2.service.V2ScanAsyncBridge;
import com.syncscore.v2.service.V2ScanOrchestrator;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v2/scans")
public class V2ScanController {

    private final V2ScanOrchestrator orchestrator;
    private final V2ScanAsyncBridge asyncBridge;
    private final ArchitectureScanRepository archScanRepo;
    private final AgencyProfileRepository agencyRepo;

    public V2ScanController(
            V2ScanOrchestrator orchestrator,
            V2ScanAsyncBridge asyncBridge,
            ArchitectureScanRepository archScanRepo,
            AgencyProfileRepository agencyRepo) {
        this.orchestrator = orchestrator;
        this.asyncBridge = asyncBridge;
        this.archScanRepo = archScanRepo;
        this.agencyRepo = agencyRepo;
    }

    @PostMapping("/trigger")
    public ArchitectureScanResponse triggerScan(@AuthenticationPrincipal AccessPrincipal principal) {
        var agency = agencyRepo.findByUserId(principal.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agency profile not found"));

        UUID archScanId = orchestrator.createQueuedScan(agency.getId(), null);
        asyncBridge.runAsync(archScanId, agency.getId(), 0, 0);

        ArchitectureScan scan = archScanRepo.findById(archScanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Scan creation failed"));
        return toResponse(scan);
    }

    @GetMapping("/{scanId}")
    public ArchitectureScanResponse getScan(
            @AuthenticationPrincipal AccessPrincipal principal,
            @PathVariable UUID scanId) {
        ArchitectureScan scan = archScanRepo.findById(scanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Architecture scan not found"));

        var agency = agencyRepo.findById(scan.getAgencyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agency not found"));
        if (!agency.getUserId().equals(principal.userId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your scan");
        }
        return toResponse(scan);
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
                scan.getCreatedAt(),
                scan.getFinishedAt()
        );
    }
}

