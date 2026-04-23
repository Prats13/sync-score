package com.syncscore.v1.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.syncscore.security.AccessPrincipal;
import com.syncscore.v1.api.dto.GithubVerificationRequest;
import com.syncscore.v1.api.dto.PasteVerificationRequest;
import com.syncscore.v1.api.dto.ScanEnqueueResponse;
import com.syncscore.v1.domain.AgencyProfile;
import com.syncscore.v1.domain.EvidenceItem;
import com.syncscore.v1.domain.EvidenceType;
import com.syncscore.v1.repo.EvidenceItemRepository;
import com.syncscore.v1.service.V1AgencyService;
import com.syncscore.v1.service.V1ScanRunner;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/verifications")
public class V1VerificationController {

    private final V1AgencyService agencyService;
    private final V1ScanRunner scanRunner;
    private final EvidenceItemRepository evidenceRepo;
    private final ObjectMapper objectMapper;

    public V1VerificationController(
            V1AgencyService agencyService,
            V1ScanRunner scanRunner,
            EvidenceItemRepository evidenceRepo,
            ObjectMapper objectMapper
    ) {
        this.agencyService = agencyService;
        this.scanRunner = scanRunner;
        this.evidenceRepo = evidenceRepo;
        this.objectMapper = objectMapper;
    }

    /**
     * Submit a GitHub username and immediately enqueue a scan.
     * Saves a GITHUB_USERNAME evidence item so the orchestrator can pick it up.
     */
    @PostMapping("/github")
    public ScanEnqueueResponse submitGithub(
            @AuthenticationPrincipal AccessPrincipal principal,
            @Valid @RequestBody GithubVerificationRequest req
    ) {
        AgencyProfile agency = agencyService.getAgencyForUserOrThrow(principal.userId());

        EvidenceItem ev = new EvidenceItem(agency.getId(), EvidenceType.GITHUB_USERNAME);
        ev.setContentText(req.githubUsername().strip());
        evidenceRepo.save(ev);

        UUID scanId = scanRunner.enqueueInitialScan(agency.getId());
        return new ScanEnqueueResponse(scanId);
    }

    /**
     * Submit pasted manifest text and immediately enqueue a scan.
     * Saves a MANIFEST_TEXT evidence item labeled as self-reported.
     */
    @PostMapping("/paste")
    public ScanEnqueueResponse submitPaste(
            @AuthenticationPrincipal AccessPrincipal principal,
            @Valid @RequestBody PasteVerificationRequest req
    ) {
        AgencyProfile agency = agencyService.getAgencyForUserOrThrow(principal.userId());

        ObjectNode payload = objectMapper.createObjectNode();
        if (req.manifestType() != null && !req.manifestType().isBlank()) {
            payload.put("manifest_type", req.manifestType().toUpperCase(Locale.ROOT));
        }

        EvidenceItem ev = new EvidenceItem(agency.getId(), EvidenceType.MANIFEST_TEXT);
        ev.setContentText(req.content());
        ev.setPayloadJson(payload);
        evidenceRepo.save(ev);

        UUID scanId = scanRunner.enqueueInitialScan(agency.getId());
        return new ScanEnqueueResponse(scanId);
    }

    @PostMapping("/{agencyId}/rescan")
    public ScanEnqueueResponse rescan(
            @AuthenticationPrincipal AccessPrincipal principal,
            @PathVariable UUID agencyId
    ) {
        agencyService.getOwnedAgencyOrThrow(principal.userId(), agencyId);
        try {
            UUID scanId = scanRunner.enqueueRescan(agencyId);
            return new ScanEnqueueResponse(scanId);
        } catch (IllegalStateException e) {
            // Rescan cap is enforced in AgencyProfile.incrementRescanCountOrThrow
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        }
    }
}
