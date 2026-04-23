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
@Table(name = "confidential_scan_sessions")
public class ConfidentialScanSession {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Column(name = "arch_scan_id", nullable = false)
    private UUID archScanId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "exclusions_json")
    private JsonNode exclusionsJson;

    @Column(name = "custom_exclusions", columnDefinition = "text")
    private String customExclusions;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ConfidentialScanSession() {}

    public ConfidentialScanSession(UUID agencyId, UUID archScanId, String sourceType,
                                   JsonNode exclusionsJson, String customExclusions) {
        this.agencyId = agencyId;
        this.archScanId = archScanId;
        this.sourceType = sourceType;
        this.exclusionsJson = exclusionsJson;
        this.customExclusions = customExclusions;
    }

    public UUID getId() { return id; }
    public UUID getAgencyId() { return agencyId; }
    public UUID getArchScanId() { return archScanId; }
    public String getSourceType() { return sourceType; }
    public JsonNode getExclusionsJson() { return exclusionsJson; }
    public String getCustomExclusions() { return customExclusions; }
    public Instant getCreatedAt() { return createdAt; }
}
