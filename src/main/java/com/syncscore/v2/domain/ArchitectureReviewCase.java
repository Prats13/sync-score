package com.syncscore.v2.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "architecture_review_cases")
public class ArchitectureReviewCase {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Column(name = "architecture_scan_id", nullable = false)
    private UUID architectureScanId;

    @Column(nullable = false, length = 20)
    private String status = "OPEN";

    @Column(name = "trigger_reason", nullable = false, length = 100)
    private String triggerReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trigger_details_json")
    private JsonNode triggerDetailsJson;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolution_note", columnDefinition = "text")
    private String resolutionNote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected ArchitectureReviewCase() {}

    public ArchitectureReviewCase(UUID agencyId, UUID architectureScanId,
                                  String triggerReason, JsonNode triggerDetailsJson) {
        this.agencyId = agencyId;
        this.architectureScanId = architectureScanId;
        this.triggerReason = triggerReason;
        this.triggerDetailsJson = triggerDetailsJson;
    }

    public void resolve(UUID resolvedBy, String note) {
        this.status = "RESOLVED";
        this.resolvedBy = resolvedBy;
        this.resolutionNote = note;
        this.resolvedAt = Instant.now();
    }

    public void dismiss(UUID resolvedBy, String note) {
        this.status = "DISMISSED";
        this.resolvedBy = resolvedBy;
        this.resolutionNote = note;
        this.resolvedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getAgencyId() { return agencyId; }
    public UUID getArchitectureScanId() { return architectureScanId; }
    public String getStatus() { return status; }
    public String getTriggerReason() { return triggerReason; }
    public JsonNode getTriggerDetailsJson() { return triggerDetailsJson; }
    public UUID getResolvedBy() { return resolvedBy; }
    public String getResolutionNote() { return resolutionNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getResolvedAt() { return resolvedAt; }
}