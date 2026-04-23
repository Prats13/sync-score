package com.syncscore.v2.api;

import com.syncscore.security.AccessPrincipal;
import com.syncscore.v2.api.dto.ReviewCaseResolveRequest;
import com.syncscore.v2.api.dto.ReviewCaseResponse;
import com.syncscore.v2.domain.ArchitectureReviewCase;
import com.syncscore.v2.service.V2ReviewCaseService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v2/admin/review-cases")
public class V2AdminController {

    private final V2ReviewCaseService reviewCaseService;

    public V2AdminController(V2ReviewCaseService reviewCaseService) {
        this.reviewCaseService = reviewCaseService;
    }

    @GetMapping
    public List<ReviewCaseResponse> listOpenCases() {
        return reviewCaseService.listOpen().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{caseId}")
    public ReviewCaseResponse getCase(@PathVariable UUID caseId) {
        return toResponse(reviewCaseService.getCase(caseId));
    }

    @PostMapping("/{caseId}/resolve")
    public ReviewCaseResponse resolve(
            @AuthenticationPrincipal AccessPrincipal principal,
            @PathVariable UUID caseId,
            @RequestBody ReviewCaseResolveRequest request) {
        if (request == null || request.action() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing action");
        }
        ArchitectureReviewCase updated = switch (request.action().trim().toUpperCase()) {
            case "APPROVE" -> reviewCaseService.approve(caseId, principal.userId(), request.note());
            case "DISMISS" -> reviewCaseService.dismiss(caseId, principal.userId(), request.note());
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action must be APPROVE or DISMISS");
        };
        return toResponse(updated);
    }

    private ReviewCaseResponse toResponse(ArchitectureReviewCase c) {
        return new ReviewCaseResponse(
                c.getId(), c.getAgencyId(), c.getArchitectureScanId(),
                c.getStatus(), c.getTriggerReason(), c.getTriggerDetailsJson(),
                c.getResolvedBy(), c.getResolutionNote(), c.getCreatedAt(), c.getResolvedAt()
        );
    }
}

