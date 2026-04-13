package com.syncscore.producer.service;

import com.syncscore.producer.domain.Badge2Status;
import com.syncscore.producer.domain.ProducerProfile;
import com.syncscore.producer.repo.ProducerProfileRepository;
import com.syncscore.producer.service.dto.ProducerProfileRequest;
import com.syncscore.producer.service.dto.ProducerProfileResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DefaultProducerService implements ProducerService {

    private final ProducerProfileRepository profileRepository;
    private final UrlVerificationService urlVerificationService;
    private final Clock clock;

    public DefaultProducerService(ProducerProfileRepository profileRepository,
                                  UrlVerificationService urlVerificationService,
                                  Clock clock) {
        this.profileRepository = profileRepository;
        this.urlVerificationService = urlVerificationService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public ProducerProfileResponse submitProfile(UUID userId, ProducerProfileRequest request) {
        ProducerProfile profile = profileRepository.findByUserId(userId)
                .map(existing -> {
                    existing.updateUrls(request.linkedinUrl(), request.githubUrl(),
                            request.websiteUrl(), request.liveProjectUrl(), Instant.now(clock));
                    return existing;
                })
                .orElseGet(() -> new ProducerProfile(userId, request.linkedinUrl(), request.githubUrl(),
                        request.websiteUrl(), request.liveProjectUrl(), Instant.now(clock)));
        return toResponse(profileRepository.save(profile));
    }

    @Override
    @Transactional(readOnly = true)
    public ProducerProfileResponse getProfile(UUID userId) {
        return toResponse(findOrThrow(userId));
    }

    @Override
    @Transactional
    public ProducerProfileResponse verifyProfile(UUID userId) {
        ProducerProfile profile = findOrThrow(userId);

        boolean linkedinOk = urlVerificationService.isReachable(profile.getLinkedinUrl());
        boolean websiteOk = urlVerificationService.isReachable(profile.getWebsiteUrl());
        boolean liveProjectOk = urlVerificationService.isReachable(profile.getLiveProjectUrl());
        Boolean githubOk = profile.getGithubUrl() != null
                ? urlVerificationService.isReachable(profile.getGithubUrl())
                : null;

        boolean passed = linkedinOk && websiteOk && liveProjectOk
                && (githubOk == null || githubOk);

        profile.recordVerification(linkedinOk, githubOk, websiteOk, liveProjectOk,
                passed ? Badge2Status.VERIFIED : Badge2Status.FAILED,
                Instant.now(clock));

        return toResponse(profileRepository.save(profile));
    }

    private ProducerProfile findOrThrow(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Profile not found — submit your profile first"));
    }

    private ProducerProfileResponse toResponse(ProducerProfile p) {
        return new ProducerProfileResponse(
                p.getId(),
                p.getLinkedinUrl(),
                p.getGithubUrl(),
                p.getWebsiteUrl(),
                p.getLiveProjectUrl(),
                p.getBadge2Status().name(),
                new ProducerProfileResponse.VerificationResult(
                        p.getLinkedinReachable(),
                        p.getGithubReachable(),
                        p.getWebsiteReachable(),
                        p.getLiveProjectReachable()
                ),
                p.getVerifiedAt(),
                p.getCreatedAt()
        );
    }
}