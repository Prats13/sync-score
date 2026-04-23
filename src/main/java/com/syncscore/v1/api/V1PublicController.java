package com.syncscore.v1.api;

import com.syncscore.v1.api.dto.BrowseAgencyResponse;
import com.syncscore.v1.api.dto.PublicAgencyProfileResponse;
import com.syncscore.v1.domain.AgencyProfile;
import com.syncscore.v1.domain.DetectedPackage;
import com.syncscore.v1.domain.PublicProfile;
import com.syncscore.v1.domain.ScoreResult;
import com.syncscore.v1.domain.SyncTier;
import com.syncscore.v1.repo.AgencyProfileRepository;
import com.syncscore.v1.repo.DetectedPackageRepository;
import com.syncscore.v1.repo.PublicProfileRepository;
import com.syncscore.v1.repo.ScoreResultRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
public class V1PublicController {

    private final PublicProfileRepository publicRepo;
    private final AgencyProfileRepository agencyRepo;
    private final ScoreResultRepository scoreRepo;
    private final DetectedPackageRepository detectedRepo;

    public V1PublicController(
            PublicProfileRepository publicRepo,
            AgencyProfileRepository agencyRepo,
            ScoreResultRepository scoreRepo,
            DetectedPackageRepository detectedRepo
    ) {
        this.publicRepo = publicRepo;
        this.agencyRepo = agencyRepo;
        this.scoreRepo = scoreRepo;
        this.detectedRepo = detectedRepo;
    }

    @GetMapping("/browse")
    public List<BrowseAgencyResponse> browse(@RequestParam(name = "tier", required = false) SyncTier tier) {
        return publicRepo.browsePublic(tier).stream()
                .map(r -> new BrowseAgencyResponse(
                        r.getSlug(),
                        r.getAgencyId(),
                        r.getAgencyName(),
                        r.getNiche(),
                        r.getWebsiteUrl(),
                        r.getBookingUrl(),
                        r.getTotalScore(),
                        r.getTier(),
                        r.getVerificationLabel()
                ))
                .toList();
    }

    @GetMapping("/public/agencies/{slug}")
    public PublicAgencyProfileResponse publicProfile(@PathVariable String slug) {
        PublicProfile pp = publicRepo.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Public profile not found"));
        if (!pp.isPublic()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Public profile not found");
        }

        AgencyProfile agency = agencyRepo.findById(pp.getAgencyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agency not found"));

        Optional<ScoreResult> score = Optional.empty();
        if (pp.getLatestScoreResultId() != null) {
            score = scoreRepo.findById(pp.getLatestScoreResultId());
        }

        List<PublicAgencyProfileResponse.StackChip> stack = List.of();
        if (score.isPresent()) {
            List<DetectedPackage> pkgs = detectedRepo.findByScanEventId(score.get().getScanEventId());
            stack = pkgs.stream()
                    .sorted(Comparator.comparingInt(DetectedPackage::getPointsAwarded).reversed()
                            .thenComparing(DetectedPackage::getPackageNameNormalized))
                    .limit(40)
                    .map(p -> new PublicAgencyProfileResponse.StackChip(
                            p.getPackageNameNormalized(),
                            p.getCategory(),
                            p.getPointsAwarded()
                    ))
                    .toList();
        }

        PublicAgencyProfileResponse.Score scoreResp = score.map(s -> new PublicAgencyProfileResponse.Score(
                s.getTotalScore(),
                s.getTier(),
                s.getRulesetVersion(),
                s.getCategorySubtotals()
        )).orElse(new PublicAgencyProfileResponse.Score(null, null, null, null));

        return new PublicAgencyProfileResponse(
                agency.getId(),
                pp.getSlug(),
                pp.getVerificationLabel(),
                new PublicAgencyProfileResponse.Agency(
                        agency.getName(),
                        agency.getNiche(),
                        agency.getWebsiteUrl(),
                        agency.getDescription(),
                        agency.getBookingUrl()
                ),
                scoreResp,
                stack
        );
    }
}

