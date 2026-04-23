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
@Table(name = "score_results")
public class ScoreResult {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "scan_event_id", nullable = false, unique = true)
    private UUID scanEventId;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncTier tier;

    @Column(name = "ruleset_version", nullable = false, length = 50)
    private String rulesetVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_subtotals")
    private JsonNode categorySubtotals;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ScoreResult() {}

    public ScoreResult(UUID scanEventId, int totalScore, SyncTier tier, String rulesetVersion) {
        this.scanEventId = scanEventId;
        this.totalScore = totalScore;
        this.tier = tier;
        this.rulesetVersion = rulesetVersion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getScanEventId() {
        return scanEventId;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public SyncTier getTier() {
        return tier;
    }

    public String getRulesetVersion() {
        return rulesetVersion;
    }

    public JsonNode getCategorySubtotals() {
        return categorySubtotals;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCategorySubtotals(JsonNode categorySubtotals) {
        this.categorySubtotals = categorySubtotals;
    }
}
