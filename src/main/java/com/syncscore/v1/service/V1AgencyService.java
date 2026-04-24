package com.syncscore.v1.service;

import com.syncscore.v1.domain.AgencyProfile;
import com.syncscore.v1.domain.PublicProfile;
import com.syncscore.v1.domain.ScanEvent;
import com.syncscore.v1.domain.VerificationLabel;
import com.syncscore.v1.repo.AgencyProfileRepository;
import com.syncscore.v1.repo.PublicProfileRepository;
import com.syncscore.v1.repo.ScanEventRepository;
import com.syncscore.v1.util.SlugUtil;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class V1AgencyService {

    private final AgencyProfileRepository agencyRepo;
    private final PublicProfileRepository publicRepo;
    private final ScanEventRepository scanEventRepo;

    public V1AgencyService(
            AgencyProfileRepository agencyRepo,
            PublicProfileRepository publicRepo,
            ScanEventRepository scanEventRepo
    ) {
        this.agencyRepo = agencyRepo;
        this.publicRepo = publicRepo;
        this.scanEventRepo = scanEventRepo;
    }

    @Transactional
    public AgencyProfile upsertAgency(UUID userId,
                                     String name,
                                     String niche,
                                     String websiteUrl,
                                     String description,
                                     String bookingUrl,
                                     String githubUsername,
                                     boolean isPublic) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }

        AgencyProfile agency = agencyRepo.findByUserId(userId)
                .orElseGet(() -> new AgencyProfile(userId, name));

        agency.updateProfile(name, niche, websiteUrl, description, bookingUrl, githubUsername, isPublic);
        AgencyProfile saved = agencyRepo.save(agency);

        ensurePublicProfileState(saved, isPublic);
        return saved;
    }

    @Transactional
    public void saveGithubUsername(UUID agencyId, String githubUsername) {
        agencyRepo.findById(agencyId).ifPresent(agency -> {
            agency.updateProfile(
                    agency.getName(), agency.getNiche(), agency.getWebsiteUrl(),
                    agency.getDescription(), agency.getBookingUrl(), githubUsername, agency.isPublic());
            agencyRepo.save(agency);
        });
    }

    @Transactional(readOnly = true)
    public AgencyProfile getAgencyForUserOrThrow(UUID userId) {
        return agencyRepo.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agency profile not found"));
    }

    @Transactional(readOnly = true)
    public AgencyProfile getOwnedAgencyOrThrow(UUID userId, UUID agencyId) {
        AgencyProfile agency = agencyRepo.findById(agencyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Agency not found"));
        if (!agency.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your agency");
        }
        return agency;
    }

    @Transactional(readOnly = true)
    public Optional<PublicProfile> getPublicProfile(UUID agencyId) {
        return publicRepo.findByAgencyId(agencyId);
    }

    @Transactional
    protected void ensurePublicProfileState(AgencyProfile agency, boolean isPublic) {
        Optional<PublicProfile> existing = publicRepo.findByAgencyId(agency.getId());
        if (!isPublic) {
            existing.ifPresent(pp -> {
                pp.unpublish();
                publicRepo.save(pp);
            });
            return;
        }

        VerificationLabel label = inferLatestVerificationLabel(agency.getId())
                .orElse(VerificationLabel.SELF_REPORTED);
        String baseSlug = SlugUtil.slugify(agency.getName());

        PublicProfile pp = existing.orElseGet(() -> {
            String unique = ensureUniqueSlug(baseSlug, null);
            return new PublicProfile(agency.getId(), unique, label);
        });

        // Allow slug to track name before first publish; lock after publishAt is set.
        String unique = ensureUniqueSlug(baseSlug, pp.getId());
        pp.setSlugIfUnpublished(unique);
        pp.setVerificationLabel(label);
        pp.publishNow(Instant.now());
        publicRepo.save(pp);
    }

    private Optional<VerificationLabel> inferLatestVerificationLabel(UUID agencyId) {
        List<ScanEvent> events = scanEventRepo.findByAgencyIdOrderByCreatedAtDesc(agencyId);
        if (events.isEmpty()) return Optional.empty();
        return Optional.ofNullable(events.getFirst().getVerificationLabel());
    }

    private String ensureUniqueSlug(String base, UUID selfPublicProfileId) {
        String candidate = base;
        int i = 0;
        while (true) {
            Optional<PublicProfile> existing = publicRepo.findBySlug(candidate);
            if (existing.isEmpty()) return candidate;
            if (selfPublicProfileId != null && existing.get().getId().equals(selfPublicProfileId)) {
                return candidate;
            }
            i += 1;
            candidate = base + "-" + i;
        }
    }
}

