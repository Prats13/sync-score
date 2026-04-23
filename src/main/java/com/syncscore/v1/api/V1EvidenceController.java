package com.syncscore.v1.api;

import com.syncscore.security.AccessPrincipal;
import com.syncscore.v1.api.dto.EvidenceCreateRequest;
import com.syncscore.v1.api.dto.EvidenceItemResponse;
import com.syncscore.v1.domain.AgencyProfile;
import com.syncscore.v1.domain.EvidenceItem;
import com.syncscore.v1.repo.EvidenceItemRepository;
import com.syncscore.v1.service.V1AgencyService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evidence")
public class V1EvidenceController {

    private final V1AgencyService agencyService;
    private final EvidenceItemRepository evidenceRepo;

    public V1EvidenceController(V1AgencyService agencyService, EvidenceItemRepository evidenceRepo) {
        this.agencyService = agencyService;
        this.evidenceRepo = evidenceRepo;
    }

    @PostMapping
    public EvidenceItemResponse createEvidence(
            @AuthenticationPrincipal AccessPrincipal principal,
            @Valid @RequestBody EvidenceCreateRequest req
    ) {
        AgencyProfile agency = agencyService.getAgencyForUserOrThrow(principal.userId());

        EvidenceItem ev = new EvidenceItem(agency.getId(), req.evidenceType());
        ev.setContentText(req.contentText());
        ev.setContentUrl(req.contentUrl());
        ev.setPayloadJson(req.payload());

        EvidenceItem saved = evidenceRepo.save(ev);
        return toResponse(saved);
    }

    private EvidenceItemResponse toResponse(EvidenceItem e) {
        return new EvidenceItemResponse(
                e.getId(),
                e.getAgencyId(),
                e.getEvidenceType(),
                e.getContentText(),
                e.getContentUrl(),
                e.getPayloadJson(),
                e.getCreatedAt()
        );
    }
}

