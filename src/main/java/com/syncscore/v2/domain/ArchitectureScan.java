package com.syncscore.v2.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "architecture_scans")
public class ArchitectureScan {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Column(name = "scan_event_id")
    private UUID scanEventId;

    @Column(nullable = false, length = 20)
    private String status;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private ArchConfidence confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "arch_status", length = 30)
    private ArchStatus archStatus;

    @Column(name = "evidence_source", nullable = false, length = 30)
    private String evidenceSource = "PUBLIC_EVIDENCE";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "structural_signals_json")
    private JsonNode structuralSignalsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "summary_json")
    private JsonNode summaryJson;

    @Column(name = "ruleset_version", nullable = false, length = 50)
    private String rulesetVersion;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected ArchitectureScan() {}

    public ArchitectureScan(UUID agencyId, UUID scanEventId, String rulesetVersion) {
        this.agencyId = agencyId;
        this.scanEventId = scanEventId;
        this.rulesetVersion = rulesetVersion;
        this.status = "QUEUED";
    }

    public void markRunning() {
        this.status = "RUNNING";
        this.startedAt = Instant.now();
    }

    public void markSucceeded(ArchConfidence confidence, ArchStatus archStatus) {
        this.status = "SUCCEEDED";
        this.confidence = confidence;
        this.archStatus = archStatus;
        this.finishedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = "FAILED";
        this.errorMessage = error;
        this.finishedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getAgencyId() { return agencyId; }
    public UUID getScanEventId() { return scanEventId; }
    public String getStatus() { return status; }
    public ArchConfidence getConfidence() { return confidence; }
    public ArchStatus getArchStatus() { return archStatus; }
    public String getEvidenceSource() { return evidenceSource; }
    public JsonNode getStructuralSignalsJson() { return structuralSignalsJson; }
    public JsonNode getSummaryJson() { return summaryJson; }
    public String getRulesetVersion() { return rulesetVersion; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }

    public void setStructuralSignalsJson(JsonNode node) { this.structuralSignalsJson = node; }
    public void setSummaryJson(JsonNode node) { this.summaryJson = node; }
}