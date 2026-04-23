package com.syncscore.v1.domain;

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
@Table(name = "scan_events")
public class ScanEvent {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 20)
    private ScanTriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ScanStatus status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "ruleset_version", nullable = false, length = 50)
    private String rulesetVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_label", nullable = false, length = 30)
    private VerificationLabel verificationLabel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence_item_ids")
    private JsonNode evidenceItemIds;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ScanEvent() {}

    public ScanEvent(UUID agencyId,
                    ScanTriggerType triggerType,
                    ScanStatus status,
                    String rulesetVersion,
                    VerificationLabel verificationLabel) {
        this.agencyId = agencyId;
        this.triggerType = triggerType;
        this.status = status;
        this.rulesetVersion = rulesetVersion;
        this.verificationLabel = verificationLabel;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAgencyId() {
        return agencyId;
    }

    public ScanTriggerType getTriggerType() {
        return triggerType;
    }

    public ScanStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getRulesetVersion() {
        return rulesetVersion;
    }

    public VerificationLabel getVerificationLabel() {
        return verificationLabel;
    }

    public JsonNode getEvidenceItemIds() {
        return evidenceItemIds;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markQueued(JsonNode evidenceItemIds) {
        this.status = ScanStatus.QUEUED;
        this.evidenceItemIds = evidenceItemIds;
        this.errorMessage = null;
    }

    public void markRunning(Instant now) {
        this.status = ScanStatus.RUNNING;
        this.startedAt = now;
        this.errorMessage = null;
    }

    public void markSucceeded(Instant now) {
        this.status = ScanStatus.SUCCEEDED;
        this.finishedAt = now;
        this.errorMessage = null;
    }

    public void markFailed(Instant now, String errorMessage) {
        this.status = ScanStatus.FAILED;
        this.finishedAt = now;
        this.errorMessage = errorMessage;
    }
}
