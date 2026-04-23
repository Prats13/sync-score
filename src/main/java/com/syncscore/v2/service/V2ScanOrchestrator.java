package com.syncscore.v2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncscore.v2.api.dto.ConfidentialScanRequest;
import com.syncscore.v2.domain.ArchStatus;
import com.syncscore.v2.domain.ArchitectureReviewCase;
import com.syncscore.v2.domain.ArchitectureScan;
import com.syncscore.v2.domain.ArchScanStructuralSignal;
import com.syncscore.v2.domain.ConfidentialScanSession;
import com.syncscore.v2.repo.ArchitectureReviewCaseRepository;
import com.syncscore.v2.repo.ArchitectureScanRepository;
import com.syncscore.v2.repo.ArchScanStructuralSignalRepository;
import com.syncscore.v2.repo.ConfidentialScanSessionRepository;
import com.syncscore.v2.scanner.StructuralSignalAggregator;
import com.syncscore.v2.scanner.V2StructuralScanner;
import com.syncscore.v2.scoring.AntiGamingEvaluator;
import com.syncscore.v2.scoring.SignalScore;
import com.syncscore.v2.scoring.V2ConfidenceScorer;
import com.syncscore.v1.repo.AgencyProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.StringUtils;

@Service
public class V2ScanOrchestrator {

    static final String RULESET_VERSION = "syncscore-v2-2026-04-18";
    private static final Logger log = LoggerFactory.getLogger(V2ScanOrchestrator.class);

    private final ArchitectureScanRepository archScanRepo;
    private final ArchScanStructuralSignalRepository signalRepo;
    private final ArchitectureReviewCaseRepository reviewCaseRepo;
    private final ConfidentialScanSessionRepository sessionRepo;
    private final AgencyProfileRepository agencyRepo;
    private final V2StructuralScanner structuralScanner;
    private final V2ConfidenceScorer confidenceScorer;
    private final AntiGamingEvaluator antiGamingEvaluator;
    private final V2ScanAsyncBridge asyncBridge;
    private final ObjectMapper objectMapper;

    public V2ScanOrchestrator(
            ArchitectureScanRepository archScanRepo,
            ArchScanStructuralSignalRepository signalRepo,
            ArchitectureReviewCaseRepository reviewCaseRepo,
            ConfidentialScanSessionRepository sessionRepo,
            AgencyProfileRepository agencyRepo,
            V2StructuralScanner structuralScanner,
            V2ConfidenceScorer confidenceScorer,
            AntiGamingEvaluator antiGamingEvaluator,
            @Lazy V2ScanAsyncBridge asyncBridge,
            ObjectMapper objectMapper) {
        this.archScanRepo = archScanRepo;
        this.signalRepo = signalRepo;
        this.reviewCaseRepo = reviewCaseRepo;
        this.sessionRepo = sessionRepo;
        this.agencyRepo = agencyRepo;
        this.structuralScanner = structuralScanner;
        this.confidenceScorer = confidenceScorer;
        this.antiGamingEvaluator = antiGamingEvaluator;
        this.asyncBridge = asyncBridge;
        this.objectMapper = objectMapper;
    }

    // IMPORTANT: Must use @TransactionalEventListener(AFTER_COMMIT) so this fires
    // after V1's transaction commits, not inside it.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onScanCompleted(ScanCompletedEvent event) {
        agencyRepo.findById(event.agencyId()).ifPresent(agency -> {
            if (!StringUtils.hasText(agency.getGithubUsername())) return;
            UUID archScanId = createQueuedScan(event.agencyId(), event.scanEventId());
            asyncBridge.runAsync(archScanId, event.agencyId(), event.detectedPackageCount(), event.newHighTierPackageCount());
        });
    }

    @Transactional
    public UUID createQueuedScan(UUID agencyId, UUID scanEventId) {
        ArchitectureScan scan = new ArchitectureScan(agencyId, scanEventId, RULESET_VERSION);
        UUID id = archScanRepo.save(scan).getId();
        log.info("event=ARCH_SCAN_QUEUED archScanId={} agencyId={}", id, agencyId);
        return id;
    }

    @Transactional
    public UUID createConfidentialScan(UUID agencyId, ConfidentialScanRequest request) {
        String evidenceSource = toEvidenceSource(request.source());
        ArchitectureScan scan = new ArchitectureScan(agencyId, null, RULESET_VERSION);
        scan.setEvidenceSource(evidenceSource);
        UUID id = archScanRepo.save(scan).getId();

        ConfidentialScanSession session = new ConfidentialScanSession(
                agencyId, id, request.source(),
                objectMapper.valueToTree(request.exclusions() != null ? request.exclusions() : java.util.List.of()),
                request.customExclusions()
        );
        sessionRepo.save(session);

        log.info("event=CONFIDENTIAL_SCAN_QUEUED archScanId={} agencyId={} source={}", id, agencyId, request.source());
        return id;
    }

    private static String toEvidenceSource(String source) {
        return switch (source == null ? "" : source.toLowerCase()) {
            case "github"    -> "CONFIDENTIAL_GITHUB";
            case "agent"     -> "CONFIDENTIAL_AGENT";
            case "docs"      -> "CONFIDENTIAL_DOCS";
            case "export"    -> "CONFIDENTIAL_EXPORT";
            case "interview" -> "CONFIDENTIAL_INTERVIEW";
            default          -> "CONFIDENTIAL_MIXED";
        };
    }

    @Transactional
    public void runScan(UUID archScanId, UUID agencyId, int detectedPackageCount, int newHighTierPackages) {
        long startNs = System.nanoTime();
        ArchitectureScan scan = archScanRepo.findById(archScanId)
                .orElseThrow(() -> new IllegalArgumentException("Architecture scan not found: " + archScanId));
        log.info("event=ARCH_SCAN_STARTED archScanId={} agencyId={}", archScanId, agencyId);
        scan.markRunning();
        archScanRepo.save(scan);

        String githubUsername = agencyRepo.findById(agencyId)
                .orElseThrow(() -> new IllegalStateException("Agency not found"))
                .getGithubUsername();

        StructuralSignalAggregator.AggregatedSignals signals =
                structuralScanner.scan(githubUsername, detectedPackageCount);

        Optional<ArchitectureScan> priorScan = archScanRepo
                .findTopByAgencyIdAndStatusOrderByCreatedAtDesc(agencyId, "SUCCEEDED");

        StructuralSignalAggregator.AggregatedSignals priorSignals = priorScan
                .map(this::loadPriorSignals)
                .orElse(new StructuralSignalAggregator.AggregatedSignals(0, 0, 0, 0, 0, 0, 0));

        int recentCommits30d = signals.totalCommits30d() > 0 ? signals.totalCommits30d() : 0;
        AntiGamingEvaluator.Result antiGaming = antiGamingEvaluator.evaluate(
                priorSignals, signals, newHighTierPackages, recentCommits30d);

        V2ConfidenceScorer.Result scored = confidenceScorer.score(signals, antiGaming.fired());

        persistSignals(scan.getId(), scored.signalScores());

        if (antiGaming.fired()) {
            ArchitectureReviewCase reviewCase = new ArchitectureReviewCase(
                    agencyId, scan.getId(), antiGaming.reason(),
                    objectMapper.valueToTree(java.util.Map.of("detail", antiGaming.detail() != null ? antiGaming.detail() : "")));
            ArchitectureReviewCase saved = reviewCaseRepo.save(reviewCase);
            log.warn("event=REVIEW_CASE_OPENED caseId={} agencyId={} reason={}",
                    saved.getId(), agencyId, saved.getTriggerReason());
        }

        scan.markSucceeded(scored.confidence(), scored.archStatus());
        long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
        log.info("event=ARCH_SCAN_SUCCEEDED archScanId={} agencyId={} durationMs={} confidence={} archStatus={}",
                archScanId, agencyId, durationMs, scored.confidence(), scored.archStatus());
        archScanRepo.save(scan);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markScanFailed(UUID archScanId, String error) {
        archScanRepo.findById(archScanId).ifPresent(scan -> {
            scan.markFailed(error);
            archScanRepo.save(scan);
            log.error("event=ARCH_SCAN_FAILED archScanId={} error={}", archScanId, error);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFreshnessLow(UUID archScanId) {
        archScanRepo.findById(archScanId).ifPresent(scan -> {
            scan.markSucceeded(scan.getConfidence(), ArchStatus.FRESHNESS_LOW);
            archScanRepo.save(scan);
        });
    }

    private void persistSignals(UUID archScanId, List<SignalScore> scores) {
        List<ArchScanStructuralSignal> rows = scores.stream()
                .map(s -> new ArchScanStructuralSignal(
                        archScanId,
                        s.signalType(),
                        s.rawValue(),
                        s.valueLabel(),
                        s.weightedContribution()))
                .toList();
        signalRepo.saveAll(rows);
    }

    private StructuralSignalAggregator.AggregatedSignals loadPriorSignals(ArchitectureScan prior) {
        List<ArchScanStructuralSignal> signals = signalRepo.findByArchitectureScanId(prior.getId());
        int commits = extractInt(signals, com.syncscore.v2.domain.StructuralSignalType.COMMIT_FREQUENCY);
        int contributors = extractInt(signals, com.syncscore.v2.domain.StructuralSignalType.CONTRIBUTOR_COUNT);
        int ageMonths = extractInt(signals, com.syncscore.v2.domain.StructuralSignalType.REPO_AGE_MONTHS);
        int depth = extractInt(signals, com.syncscore.v2.domain.StructuralSignalType.FOLDER_DEPTH);
        int services = extractInt(signals, com.syncscore.v2.domain.StructuralSignalType.SERVICE_COUNT);
        int consistency = extractInt(signals, com.syncscore.v2.domain.StructuralSignalType.MANIFEST_CONSISTENCY);
        return new StructuralSignalAggregator.AggregatedSignals(0, commits, contributors, ageMonths, depth, services, consistency);
    }

    private int extractInt(List<ArchScanStructuralSignal> signals, com.syncscore.v2.domain.StructuralSignalType type) {
        return signals.stream()
                .filter(s -> s.getSignalType() == type)
                .findFirst()
                .map(s -> s.getValueNumeric() != null ? s.getValueNumeric().intValue() : 0)
                .orElse(0);
    }
}
