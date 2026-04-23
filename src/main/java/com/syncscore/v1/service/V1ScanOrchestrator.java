package com.syncscore.v1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.syncscore.v1.domain.AgencyProfile;
import com.syncscore.v1.domain.DetectedPackage;
import com.syncscore.v1.domain.EvidenceItem;
import com.syncscore.v1.domain.EvidenceType;
import com.syncscore.v1.domain.ManifestType;
import com.syncscore.v1.domain.PublicProfile;
import com.syncscore.v1.domain.RepositoryScan;
import com.syncscore.v1.domain.RepositoryScanStatus;
import com.syncscore.v1.domain.ScanEvent;
import com.syncscore.v1.domain.ScanStatus;
import com.syncscore.v1.domain.ScanTriggerType;
import com.syncscore.v1.domain.ScoreResult;
import com.syncscore.v1.domain.SubmissionStatus;
import com.syncscore.v1.domain.SyncTier;
import com.syncscore.v1.domain.VerificationLabel;
import com.syncscore.v1.domain.VerificationSourceType;
import com.syncscore.v1.domain.VerificationSubmission;
import com.syncscore.v1.repo.AgencyProfileRepository;
import com.syncscore.v1.repo.DetectedPackageRepository;
import com.syncscore.v1.repo.EvidenceItemRepository;
import com.syncscore.v1.repo.PublicProfileRepository;
import com.syncscore.v1.repo.RepositoryScanRepository;
import com.syncscore.v1.repo.ScanEventRepository;
import com.syncscore.v1.repo.ScoreResultRepository;
import com.syncscore.v1.repo.VerificationSubmissionRepository;
import com.syncscore.v1.scanner.GitHubScanResult;
import com.syncscore.v1.scanner.GitHubUsernameScanner;
import com.syncscore.v1.scoring.ManifestParsers;
import com.syncscore.v1.scoring.SyncScoreScorer;
import com.syncscore.v1.scoring.rules.SyncScoreRulesetLoader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import com.syncscore.v2.service.ScanCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class V1ScanOrchestrator {

    private static final int HARD_MAX_REPO_LIMIT = 15;
    private static final Logger log = LoggerFactory.getLogger(V1ScanOrchestrator.class);

    private final AgencyProfileRepository agencyRepo;
    private final EvidenceItemRepository evidenceRepo;
    private final VerificationSubmissionRepository submissionRepo;
    private final ScanEventRepository scanEventRepo;
    private final RepositoryScanRepository repositoryScanRepo;
    private final DetectedPackageRepository detectedPackageRepo;
    private final ScoreResultRepository scoreResultRepo;
    private final PublicProfileRepository publicProfileRepo;

    private final GitHubUsernameScanner gitHubScanner;
    private final ObjectMapper objectMapper;

    private final SyncScoreRulesetLoader.LoadedRuleset ruleset;
    private final SyncScoreScorer scorer;
    private final ManifestParsers parsers;

    private final ApplicationEventPublisher eventPublisher;

    public V1ScanOrchestrator(
            AgencyProfileRepository agencyRepo,
            EvidenceItemRepository evidenceRepo,
            VerificationSubmissionRepository submissionRepo,
            ScanEventRepository scanEventRepo,
            RepositoryScanRepository repositoryScanRepo,
            DetectedPackageRepository detectedPackageRepo,
            ScoreResultRepository scoreResultRepo,
            PublicProfileRepository publicProfileRepo,
            GitHubUsernameScanner gitHubScanner,
            ObjectMapper objectMapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this.agencyRepo = agencyRepo;
        this.evidenceRepo = evidenceRepo;
        this.submissionRepo = submissionRepo;
        this.scanEventRepo = scanEventRepo;
        this.repositoryScanRepo = repositoryScanRepo;
        this.detectedPackageRepo = detectedPackageRepo;
        this.scoreResultRepo = scoreResultRepo;
        this.publicProfileRepo = publicProfileRepo;
        this.gitHubScanner = gitHubScanner;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;

        this.ruleset = new SyncScoreRulesetLoader(objectMapper).loadDefault();
        this.scorer = new SyncScoreScorer(ruleset);
        this.parsers = new ManifestParsers(objectMapper);
    }

    @Transactional
    public UUID createQueuedScan(UUID agencyId, ScanTriggerType triggerType) {
        AgencyProfile agency = agencyRepo.findById(agencyId)
                .orElseThrow(() -> new IllegalArgumentException("Agency not found"));

        if (triggerType == ScanTriggerType.RESCAN) {
            agency.incrementRescanCountOrThrow();
            agencyRepo.save(agency);
        }

        List<EvidenceItem> evidence = evidenceRepo.findByAgencyIdOrderByCreatedAtDesc(agencyId);
        List<UUID> evidenceIds = evidence.stream().map(EvidenceItem::getId).toList();

        EvidencePlan plan = buildPlan(agency, evidence);

        VerificationSubmission submission = new VerificationSubmission(
                agencyId,
                plan.sourceType,
                SubmissionStatus.PENDING
        );
        submissionRepo.save(submission);

        ScanEvent event = new ScanEvent(
                agencyId,
                triggerType,
                ScanStatus.QUEUED,
                ruleset.ruleset().rulesetVersion(),
                plan.verificationLabel
        );
        event.markQueued(toEvidenceIdJson(evidenceIds));
        scanEventRepo.save(event);
        log.info("event=SCAN_QUEUED scanEventId={} agencyId={} triggerType={}",
                event.getId(), agencyId, triggerType);
        return event.getId();
    }

    /**
     * Runs the full scan inside a single transaction. If anything throws, the entire
     * transaction rolls back (no partial rows survive). The caller (V1ScanAsyncBridge)
     * is responsible for catching the exception and calling markScanFailed in a
     * separate REQUIRES_NEW transaction so the failure state is always persisted.
     */
    @Transactional
    public void runScan(UUID scanEventId) {
        long startNs = System.nanoTime();
        ScanEvent event = scanEventRepo.findById(scanEventId)
                .orElseThrow(() -> new IllegalArgumentException("Scan event not found"));

        log.info("event=SCAN_STARTED scanEventId={}", event.getId());
        event.markRunning(Instant.now());
        scanEventRepo.save(event);

        AgencyProfile agency = agencyRepo.findById(event.getAgencyId())
                .orElseThrow(() -> new IllegalStateException("Agency not found for scan event"));

        List<UUID> evidenceIds = parseEvidenceIds(event.getEvidenceItemIds());
        List<EvidenceItem> evidence = evidenceIds.isEmpty()
                ? evidenceRepo.findByAgencyIdOrderByCreatedAtDesc(event.getAgencyId())
                : evidenceRepo.findAllById(evidenceIds);

        EvidencePlan plan = buildPlan(agency, evidence);

        ScanComputation computation = switch (plan.sourceType) {
            case GITHUB -> runGitHubScan(event, agency, plan);
            case PASTE -> runPasteScan(event, plan);
        };

        persistResults(event, computation);

        event.markSucceeded(Instant.now());
        long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
        log.info("event=SCAN_SUCCEEDED scanEventId={} durationMs={}", event.getId(), durationMs);
        scanEventRepo.save(event);
        markLatestSubmission(event.getAgencyId(), SubmissionStatus.PROCESSED);

        int packageCount = computation.scored().detectedPackages().size();
        int highTierNewCount = computeNewHighTierPackageCount(event.getAgencyId(), event.getId(), computation);
        // NOTE: V2 listener MUST use @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
        // so it fires after this V1 transaction commits, not inside it.
        eventPublisher.publishEvent(new ScanCompletedEvent(
                event.getId(), event.getAgencyId(), packageCount, highTierNewCount));
    }

    private int computeNewHighTierPackageCount(UUID agencyId, UUID currentScanEventId, ScanComputation computation) {
        // "High-tier" in V1 == non-base categories (8+ points each in the current ruleset).
        final int highTierPointsThreshold = 8;

        UUID priorScanEventId = scanEventRepo.findByAgencyIdOrderByCreatedAtDesc(agencyId).stream()
                .filter(se -> !se.getId().equals(currentScanEventId))
                .filter(se -> se.getStatus() == ScanStatus.SUCCEEDED)
                .map(ScanEvent::getId)
                .findFirst()
                .orElse(null);

        Set<String> priorPackages = priorScanEventId == null
                ? Set.of()
                : detectedPackageRepo.findByScanEventId(priorScanEventId).stream()
                        .map(DetectedPackage::getPackageNameNormalized)
                        .collect(java.util.stream.Collectors.toSet());

        if (computation == null || computation.scored == null || computation.scored.detectedPackages() == null) {
            return 0;
        }

        return (int) computation.scored.detectedPackages().stream()
                .filter(p -> p.pointsAwarded() >= highTierPointsThreshold)
                .filter(p -> !priorPackages.contains(p.packageName()))
                .count();
    }

    /**
     * Records scan failure in a new transaction, independent of any rolled-back outer
     * transaction. Called by V1ScanAsyncBridge when runScan throws.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markScanFailed(UUID scanEventId, String error) {
        scanEventRepo.findById(scanEventId).ifPresent(event -> {
            event.markFailed(Instant.now(), error);
            scanEventRepo.save(event);
            markLatestSubmission(event.getAgencyId(), SubmissionStatus.FAILED);
            log.error("event=SCAN_FAILED scanEventId={} error={}", event.getId(), error);
        });
    }

    private EvidencePlan buildPlan(AgencyProfile agency, List<EvidenceItem> evidence) {
        String username = findGithubUsername(evidence);
        if (!StringUtils.hasText(username)) {
            username = agency.getGithubUsername();
        }

        if (StringUtils.hasText(username)) {
            return new EvidencePlan(VerificationSourceType.GITHUB, VerificationLabel.GITHUB_VERIFIED, username, List.of());
        }

        List<ManifestEvidence> manifests = extractManifestEvidence(evidence);
        if (manifests.isEmpty()) {
            throw new IllegalStateException("No scorable evidence found. Provide GitHub username or manifest text.");
        }
        return new EvidencePlan(VerificationSourceType.PASTE, VerificationLabel.SELF_REPORTED, null, manifests);
    }

    private ScanComputation runGitHubScan(ScanEvent event, AgencyProfile agency, EvidencePlan plan) {
        int requestedLimit = normalizeRepoLimit(agency.getRepoScanLimit());

        GitHubScanResult scan = gitHubScanner.scanUsername(
                plan.githubUsername,
                requestedLimit,
                (type, content) -> type == ManifestType.PACKAGE_JSON
                        ? parsers.parsePackageJson(content)
                        : parsers.parseRequirementsTxt(content)
        );

        List<RepositoryEvidence> repos = new ArrayList<>();
        Map<String, SourceRef> firstSeen = new HashMap<>();
        Set<String> allPackages = new HashSet<>();

        for (GitHubScanResult.ScannedRepo r : scan.scannedRepos()) {
            String fullName = r.owner() + "/" + r.name();
            RepositoryEvidence repoEv = new RepositoryEvidence(fullName, r.htmlUrl(), r.defaultBranch(), r.manifestsFound());
            repos.add(repoEv);

            for (GitHubScanResult.ManifestFile mf : r.manifestsFound()) {
                if (mf.packages() == null) continue;
                for (String pkg : mf.packages()) {
                    if (!StringUtils.hasText(pkg)) continue;
                    allPackages.add(pkg.toLowerCase(Locale.ROOT));
                    firstSeen.putIfAbsent(pkg.toLowerCase(Locale.ROOT), new SourceRef(fullName, mf.type(), mf.path()));
                }
            }
        }

        SyncScoreScorer.SyncScoreResult scored = scorer.scorePackages(allPackages);
        return new ScanComputation(plan.verificationLabel, repos, firstSeen, scored);
    }

    private ScanComputation runPasteScan(ScanEvent event, EvidencePlan plan) {
        Map<String, SourceRef> firstSeen = new HashMap<>();
        Set<String> allPackages = new HashSet<>();

        for (ManifestEvidence m : plan.manifests) {
            Set<String> pkgs = m.manifestType == ManifestType.PACKAGE_JSON
                    ? parsers.parsePackageJson(m.content)
                    : parsers.parseRequirementsTxt(m.content);
            for (String p : pkgs) {
                allPackages.add(p);
                firstSeen.putIfAbsent(p, new SourceRef(null, m.manifestType, null));
            }
        }

        SyncScoreScorer.SyncScoreResult scored = scorer.scorePackages(allPackages);
        return new ScanComputation(plan.verificationLabel, List.of(), firstSeen, scored);
    }

    @Transactional
    protected void persistResults(ScanEvent event, ScanComputation computation) {
        // 1) Persist per-repo scan evidence
        Map<String, UUID> repoNameToId = new HashMap<>();
        for (RepositoryEvidence r : computation.repos) {
            RepositoryScan rs = new RepositoryScan(event.getId(), r.repoFullName, r.repoUrl, RepositoryScanStatus.SCANNED);
            rs.setDefaultBranch(r.defaultBranch);
            rs.setManifestsFound(toManifestsJson(r.manifestsFound));
            if (r.manifestsFound == null || r.manifestsFound.isEmpty()) {
                rs.markSkipped();
            } else {
                rs.markScanned();
            }
            RepositoryScan saved = repositoryScanRepo.save(rs);
            repoNameToId.put(r.repoFullName, saved.getId());
        }

        // 2) Persist score result
        SyncTier tier = parseTier(computation.scored.tier());
        ScoreResult score = new ScoreResult(event.getId(), computation.scored.totalScore(), tier, event.getRulesetVersion());
        score.setCategorySubtotals(objectMapper.valueToTree(computation.scored.categorySubtotals()));
        ScoreResult savedScore = scoreResultRepo.save(score);

        // 3) Persist detected packages (points + category) with best-effort source ref
        List<DetectedPackage> rows = new ArrayList<>();
        for (SyncScoreScorer.DetectedPackage p : computation.scored.detectedPackages()) {
            SourceRef ref = computation.firstSeenSource.get(p.packageName());
            UUID repoScanId = null;
            ManifestType manifestType = ManifestType.PACKAGE_JSON;
            String manifestPath = null;
            if (ref != null) {
                if (ref.repoFullName != null) {
                    repoScanId = repoNameToId.get(ref.repoFullName);
                }
                manifestType = ref.manifestType;
                manifestPath = ref.manifestPath;
            }
            rows.add(new DetectedPackage(
                    event.getId(),
                    repoScanId,
                    manifestType,
                    manifestPath,
                    p.packageName(),
                    p.category(),
                    p.pointsAwarded()
            ));
        }
        detectedPackageRepo.saveAll(rows);

        // 4) If a public profile exists, keep its display snapshot updated.
        Optional<PublicProfile> publicProfile = publicProfileRepo.findByAgencyId(event.getAgencyId());
        publicProfile.ifPresent(pp -> {
            pp.setLatestScoreResultId(savedScore.getId());
            pp.setVerificationLabel(computation.verificationLabel);
            publicProfileRepo.save(pp);
        });
    }

    private String findGithubUsername(List<EvidenceItem> evidence) {
        // evidence is ordered by created_at desc; first match is the most recent submission.
        return evidence.stream()
                .filter(e -> e.getEvidenceType() == EvidenceType.GITHUB_USERNAME)
                .map(EvidenceItem::getContentText)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private List<ManifestEvidence> extractManifestEvidence(List<EvidenceItem> evidence) {
        List<ManifestEvidence> out = new ArrayList<>();
        for (EvidenceItem e : evidence) {
            if (e.getEvidenceType() != EvidenceType.MANIFEST_TEXT) continue;
            if (!StringUtils.hasText(e.getContentText())) continue;

            ManifestType mt = inferManifestType(e);
            out.add(new ManifestEvidence(mt, e.getContentText()));
        }
        return out;
    }

    private ManifestType inferManifestType(EvidenceItem e) {
        JsonNode payload = e.getPayloadJson();
        if (payload != null) {
            JsonNode mt = payload.get("manifest_type");
            if (mt == null) mt = payload.get("manifestType");
            if (mt != null && mt.isTextual()) {
                String s = mt.asText("").trim().toUpperCase(Locale.ROOT);
                if ("REQUIREMENTS_TXT".equals(s) || "REQUIREMENTS.TXT".equals(s) || "REQUIREMENTS".equals(s)) {
                    return ManifestType.REQUIREMENTS_TXT;
                }
                if ("PACKAGE_JSON".equals(s) || "PACKAGE.JSON".equals(s) || "PACKAGE".equals(s)) {
                    return ManifestType.PACKAGE_JSON;
                }
            }
        }

        // Heuristic fallback: JSON-looking -> package.json
        String t = e.getContentText().stripLeading();
        if (t.startsWith("{")) {
            return ManifestType.PACKAGE_JSON;
        }
        return ManifestType.REQUIREMENTS_TXT;
    }

    private int normalizeRepoLimit(int configuredLimit) {
        int limit = configuredLimit <= 0 ? 10 : configuredLimit;
        if (limit > HARD_MAX_REPO_LIMIT) limit = HARD_MAX_REPO_LIMIT;
        return limit;
    }

    private ArrayNode toEvidenceIdJson(List<UUID> ids) {
        ArrayNode arr = objectMapper.createArrayNode();
        for (UUID id : ids) {
            arr.add(id.toString());
        }
        return arr;
    }

    private List<UUID> parseEvidenceIds(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<UUID> out = new ArrayList<>();
        for (JsonNode v : node) {
            if (v == null || !v.isTextual()) continue;
            try {
                out.add(UUID.fromString(v.asText()));
            } catch (Exception ignored) {
            }
        }
        return out;
    }

    private JsonNode toManifestsJson(List<GitHubScanResult.ManifestFile> manifests) {
        ArrayNode arr = objectMapper.createArrayNode();
        if (manifests == null) return arr;
        for (GitHubScanResult.ManifestFile mf : manifests) {
            ObjectNode o = objectMapper.createObjectNode();
            o.put("type", mf.type().name());
            o.put("path", mf.path());
            arr.add(o);
        }
        return arr;
    }

    private SyncTier parseTier(String tierName) {
        if (!StringUtils.hasText(tierName)) {
            throw new IllegalStateException("Missing tier");
        }
        String norm = tierName.trim().toUpperCase(Locale.ROOT);
        return switch (norm) {
            case "WRAPPER" -> SyncTier.WRAPPER;
            case "BUILDER" -> SyncTier.BUILDER;
            case "EXPERT" -> SyncTier.EXPERT;
            default -> throw new IllegalStateException("Unknown tier: " + tierName);
        };
    }

    private record EvidencePlan(
            VerificationSourceType sourceType,
            VerificationLabel verificationLabel,
            String githubUsername,
            List<ManifestEvidence> manifests
    ) {}

    private record ManifestEvidence(
            ManifestType manifestType,
            String content
    ) {}

    private record RepositoryEvidence(
            String repoFullName,
            String repoUrl,
            String defaultBranch,
            List<GitHubScanResult.ManifestFile> manifestsFound
    ) {}

    private record SourceRef(
            String repoFullName,
            ManifestType manifestType,
            String manifestPath
    ) {}

    private record ScanComputation(
            VerificationLabel verificationLabel,
            List<RepositoryEvidence> repos,
            Map<String, SourceRef> firstSeenSource,
            SyncScoreScorer.SyncScoreResult scored
    ) {}

    private void markLatestSubmission(UUID agencyId, SubmissionStatus status) {
        // Best-effort: this table is mostly for audit; V1 doesn't yet join scan_events to submissions.
        submissionRepo.findTopByAgencyIdOrderByCreatedAtDesc(agencyId).ifPresent(s -> {
            if (status == SubmissionStatus.PROCESSED) {
                s.markProcessed();
            } else if (status == SubmissionStatus.FAILED) {
                s.markFailed();
            }
            submissionRepo.save(s);
        });
    }
}
