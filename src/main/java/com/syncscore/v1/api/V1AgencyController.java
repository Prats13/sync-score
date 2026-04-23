package com.syncscore.v1.api;

import com.syncscore.security.AccessPrincipal;
import com.syncscore.v1.api.dto.AgencyDashboardResponse;
import com.syncscore.v1.api.dto.AgencyResponse;
import com.syncscore.v1.api.dto.AgencyUpsertRequest;
import com.syncscore.v1.domain.AgencyProfile;
import com.syncscore.v1.domain.PublicProfile;
import com.syncscore.v1.domain.ScanEvent;
import com.syncscore.v1.domain.ScoreResult;
import com.syncscore.v1.repo.ScanEventRepository;
import com.syncscore.v1.repo.ScoreResultRepository;
import com.syncscore.v1.service.V1AgencyService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agencies")
public class V1AgencyController {

    private final V1AgencyService agencyService;
    private final ScanEventRepository scanEventRepo;
    private final ScoreResultRepository scoreRepo;

    public V1AgencyController(
            V1AgencyService agencyService,
            ScanEventRepository scanEventRepo,
            ScoreResultRepository scoreRepo
    ) {
        this.agencyService = agencyService;
        this.scanEventRepo = scanEventRepo;
        this.scoreRepo = scoreRepo;
    }

    @GetMapping("/me")
    public AgencyResponse me(@AuthenticationPrincipal AccessPrincipal principal) {
        AgencyProfile agency = agencyService.getAgencyForUserOrThrow(principal.userId());
        return toAgencyResponse(agency);
    }

    @PostMapping
    public AgencyResponse upsert(
            @AuthenticationPrincipal AccessPrincipal principal,
            @Valid @RequestBody AgencyUpsertRequest req
    ) {
        boolean isPublic = req.isPublic() != null && req.isPublic();
        AgencyProfile agency = agencyService.upsertAgency(
                principal.userId(),
                req.name(),
                req.niche(),
                req.websiteUrl(),
                req.description(),
                req.bookingUrl(),
                req.githubUsername(),
                isPublic
        );
        return toAgencyResponse(agency);
    }

    @GetMapping("/{agencyId}/dashboard")
    public AgencyDashboardResponse dashboard(
            @AuthenticationPrincipal AccessPrincipal principal,
            @PathVariable UUID agencyId
    ) {
        AgencyProfile agency = agencyService.getOwnedAgencyOrThrow(principal.userId(), agencyId);
        AgencyResponse agencyResp = toAgencyResponse(agency);

        List<ScanEvent> scans = scanEventRepo.findByAgencyIdOrderByCreatedAtDesc(agency.getId());
        List<AgencyDashboardResponse.ScanSummary> scanSummaries = scans.stream()
                .map(s -> new AgencyDashboardResponse.ScanSummary(
                        s.getId(),
                        s.getTriggerType(),
                        s.getStatus(),
                        s.getVerificationLabel(),
                        s.getCreatedAt()
                ))
                .toList();

        AgencyDashboardResponse.LatestScore latest = null;
        if (!scans.isEmpty()) {
            ScanEvent latestScan = scans.getFirst();
            Optional<ScoreResult> score = scoreRepo.findByScanEventId(latestScan.getId());
            if (score.isPresent()) {
                latest = new AgencyDashboardResponse.LatestScore(
                        score.get().getTotalScore(),
                        score.get().getTier(),
                        latestScan.getVerificationLabel(),
                        latestScan.getId(),
                        score.get().getRulesetVersion(),
                        score.get().getCreatedAt()
                );
            } else {
                latest = new AgencyDashboardResponse.LatestScore(
                        null,
                        null,
                        latestScan.getVerificationLabel(),
                        latestScan.getId(),
                        null,
                        latestScan.getCreatedAt()
                );
            }
        }

        return new AgencyDashboardResponse(agencyResp, latest, scanSummaries);
    }

    private AgencyResponse toAgencyResponse(AgencyProfile a) {
        Optional<PublicProfile> pp = agencyService.getPublicProfile(a.getId());
        String slug = pp.map(PublicProfile::getSlug).orElse(null);
        return new AgencyResponse(
                a.getId(),
                a.getName(),
                a.getNiche(),
                a.getWebsiteUrl(),
                a.getDescription(),
                a.getBookingUrl(),
                a.getGithubUsername(),
                a.isPublic(),
                slug,
                a.getRescanCount(),
                a.getRescanLimit(),
                a.getRepoScanLimit(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }
}

