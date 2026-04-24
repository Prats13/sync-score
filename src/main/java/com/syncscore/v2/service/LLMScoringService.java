package com.syncscore.v2.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syncscore.config.BedrockProperties;
import com.syncscore.v1.domain.DetectedPackage;
import com.syncscore.v1.repo.DetectedPackageRepository;
import com.syncscore.v2.domain.ArchConfidence;
import com.syncscore.v2.domain.ArchScanRepo;
import com.syncscore.v2.domain.ArchScanStructuralSignal;
import com.syncscore.v2.domain.ArchStatus;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

@Service
public class LLMScoringService {

    private static final Logger log = LoggerFactory.getLogger(LLMScoringService.class);

    private final BedrockRuntimeClient bedrockClient;
    private final BedrockProperties bedrockProps;
    private final DetectedPackageRepository detectedPackageRepo;
    private final ObjectMapper objectMapper;

    public LLMScoringService(
            BedrockRuntimeClient bedrockClient,
            BedrockProperties bedrockProps,
            DetectedPackageRepository detectedPackageRepo,
            ObjectMapper objectMapper) {
        this.bedrockClient = bedrockClient;
        this.bedrockProps = bedrockProps;
        this.detectedPackageRepo = detectedPackageRepo;
        this.objectMapper = objectMapper;
    }

    public record LLMResult(Integer score, String reasoning, String modelId) {}

    public LLMResult score(
            UUID scanEventId,
            List<ArchScanRepo> repos,
            List<ArchScanStructuralSignal> signals,
            ArchConfidence confidence,
            ArchStatus archStatus,
            double structuralWeightedTotal) {

        String prompt = buildPrompt(scanEventId, repos, signals, confidence, archStatus, structuralWeightedTotal);

        try {
            ConverseResponse response = bedrockClient.converse(ConverseRequest.builder()
                    .modelId(bedrockProps.modelId())
                    .messages(Message.builder()
                            .role(ConversationRole.USER)
                            .content(ContentBlock.fromText(prompt))
                            .build())
                    .inferenceConfig(InferenceConfiguration.builder()
                            .maxTokens(512)
                            .temperature(0.2f)
                            .topP(0.9f)
                            .build())
                    .build());

            String raw = response.output().message().content().get(0).text();
            return parseResponse(raw);
        } catch (Exception e) {
            log.error("event=LLM_SCORE_FAILED modelId={} error={}", bedrockProps.modelId(), e.getMessage());
            return null;
        }
    }

    private String buildPrompt(
            UUID scanEventId,
            List<ArchScanRepo> repos,
            List<ArchScanStructuralSignal> signals,
            ArchConfidence confidence,
            ArchStatus archStatus,
            double structuralWeightedTotal) {

        StringBuilder sb = new StringBuilder();
        sb.append("You are an AI architecture analyst evaluating an AI agent/application vendor's technical depth.\n\n");
        sb.append("You will be given structured evidence about the vendor's GitHub repositories. ");
        sb.append("Score their overall engineering sophistication from 0 to 100 where:\n");
        sb.append("- 0-39: Wrapper tier (thin API wrapper, no real engineering)\n");
        sb.append("- 40-69: Builder tier (genuine software, some integration depth)\n");
        sb.append("- 70-100: Expert tier (deep architecture, observability, multi-repo, active team)\n\n");

        // Structural confidence
        sb.append("## Structural Signals\n");
        sb.append(String.format("Structural confidence: %s | Arch status: %s | Weighted score: %.1f/100\n\n",
                confidence, archStatus, structuralWeightedTotal));

        for (var sig : signals) {
            sb.append(String.format("- %s: %s (%s contribution)\n",
                    sig.getSignalType().name(),
                    sig.getValueLabel(),
                    sig.getConfidenceContribution() != null ? sig.getConfidenceContribution().toPlainString() : "0"));
        }

        // Repos
        if (repos != null && !repos.isEmpty()) {
            sb.append("\n## Repositories Scanned\n");
            for (var r : repos) {
                sb.append(String.format("- %s: %d commits/30d, %d commits/90d, %d contributors, depth=%d, services=%d, age=%d months\n",
                        r.getRepoFullName(), r.getCommits30d(), r.getCommits90d(),
                        r.getContributorCount(), r.getMaxFolderDepth(), r.getServiceCount(), r.getRepoAgeMonths()));
            }
        }

        // Detected packages
        if (scanEventId != null) {
            List<DetectedPackage> packages = detectedPackageRepo.findByScanEventId(scanEventId);
            if (!packages.isEmpty()) {
                sb.append("\n## Detected AI Packages\n");
                String pkgList = packages.stream()
                        .collect(Collectors.groupingBy(DetectedPackage::getCategory))
                        .entrySet().stream()
                        .map(e -> e.getKey() + ": " + e.getValue().stream()
                                .map(DetectedPackage::getPackageNameNormalized)
                                .collect(Collectors.joining(", ")))
                        .collect(Collectors.joining("\n"));
                sb.append(pkgList).append("\n");
            }
        }

        sb.append("\n## Your Task\n");
        sb.append("Holistically evaluate the vendor considering: code activity, team size, architectural depth, ");
        sb.append("AI package sophistication, and production readiness signals.\n\n");
        sb.append("Respond ONLY with a JSON object on a single line, exactly like this:\n");
        sb.append("{\"score\": <0-100 integer>, \"reasoning\": \"<1-2 sentence explanation>\"}\n");
        sb.append("Do not include any other text, markdown, or explanation outside the JSON.");

        return sb.toString();
    }

    private LLMResult parseResponse(String raw) {
        try {
            // Strip markdown code fences if present
            String cleaned = raw.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```[a-z]*\\n?", "").replace("```", "").trim();
            }
            // Find JSON object
            int start = cleaned.indexOf('{');
            int end = cleaned.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }
            JsonNode node = objectMapper.readTree(cleaned);
            int score = Math.max(0, Math.min(100, node.get("score").asInt()));
            String reasoning = node.has("reasoning") ? node.get("reasoning").asText() : "";
            return new LLMResult(score, reasoning, bedrockProps.modelId());
        } catch (Exception e) {
            log.warn("event=LLM_PARSE_FAILED raw={}", raw.length() > 200 ? raw.substring(0, 200) : raw);
            return null;
        }
    }
}