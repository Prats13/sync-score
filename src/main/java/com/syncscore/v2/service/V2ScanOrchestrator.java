package com.syncscore.v2.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncscore.v2.domain.ArchitectureReviewCase;
import com.syncscore.v2.domain.ArchitectureScan;
import com.syncscore.v2.domain.ArchScanStructuralSignal;
import com.syncscore.v2.repo.ArchitectureReviewCaseRepository;
import com.syncscore.v2.repo.ArchitectureScanRepository;
import com.syncscore.v2.repo.ArchScanStructuralSignalRepository;
import com.syncscore.v2.scanner.StructuralSignalAggregator;
import com.syncscore.v2.scanner.V2StructuralScanner;
import com.syncscore.v2.scoring.AntiGamingEvaluator;
import com.syncscore.v2.scoring.SignalScore;
import com.syncscore.v2.scoring.V2ConfidenceScorer;
import com.syncscore.v1.repo.AgencyProfileRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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

    private final ArchitectureScanRepository archScanRepo;
    private final ArchScanStructuralSignalRepository signalRepo;
    private final ArchitectureReviewCaseRepository reviewCaseRepo;
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
            AgencyProfileRepository agencyRepo,
            V2StructuralScanner structuralScanner,
            V2ConfidenceScorer confidenceScorer,
            AntiGamingEvaluator antiGamingEvaluator,
            @Lazy V2ScanAsyncBridge asyncBridge,
            ObjectMapper objectMapper) {
        this.archScanRepo = archScanRepo;
        this.signalRepo = signalRepo;
        this.reviewCaseRepo = reviewCaseRepo;
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
        return archScanRepo.save(scan).getId();
    }

    @Transactional
    public void runScan(UUID archScanId, UUID agencyId, int detectedPackageCount, int newHighTierPackages) {
        ArchitectureScan scan = archScanRepo.findById(archScanId)
                .orElseThrow(() -> new IllegalArgumentException("Architecture scan not found: " + archScanId));
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
                .orElse(new StructuralSignalAggregator.AggregatedSignals(0, 0, 0, 0, 0, 0));

        int recentCommits90d = signals.totalCommits90d() > 0 ? signals.totalCommits90d() : 0;
        AntiGamingEvaluator.Result antiGaming = antiGamingEvaluator.evaluate(
                priorSignals, signals, newHighTierPackages, recentCommits90d);

        V2ConfidenceScorer.Result scored = confidenceScorer.score(signals, antiGaming.fired());

        persistSignals(scan.getId(), scored.signalScores());

        if (antiGaming.fired()) {
            ArchitectureReviewCase reviewCase = new ArchitectureReviewCase(
                    agencyId, scan.getId(), antiGaming.reason(),
                    objectMapper.valueToTree(java.util.Map.of("detail", antiGaming.detail() != null ? antiGaming.detail() : "")));
            reviewCaseRepo.save(reviewCase);
        }

        scan.markSucceeded(scored.confidence(), scored.archStatus());
        archScanRepo.save(scan);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markScanFailed(UUID archScanId, String error) {
        archScanRepo.findById(archScanId).ifPresent(scan -> {
            scan.markFailed(error);
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
        return new StructuralSignalAggregator.AggregatedSignals(commits, contributors, ageMonths, depth, services, consistency);
    }

    private int extractInt(List<ArchScanStructuralSignal> signals, com.syncscore.v2.domain.StructuralSignalType type) {
        return signals.stream()
                .filter(s -> s.getSignalType() == type)
                .findFirst()
                .map(s -> s.getValueNumeric() != null ? s.getValueNumeric().intValue() : 0)
                .orElse(0);
    }
}
