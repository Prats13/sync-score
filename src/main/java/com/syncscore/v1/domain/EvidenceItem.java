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
@Table(name = "evidence_items")
public class EvidenceItem {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 50)
    private EvidenceType evidenceType;

    @Column(name = "content_text", columnDefinition = "text")
    private String contentText;

    @Column(name = "content_url", length = 1000)
    private String contentUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json")
    private JsonNode payloadJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EvidenceItem() {}

    public EvidenceItem(UUID agencyId, EvidenceType evidenceType) {
        this.agencyId = agencyId;
        this.evidenceType = evidenceType;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAgencyId() {
        return agencyId;
    }

    public EvidenceType getEvidenceType() {
        return evidenceType;
    }

    public String getContentText() {
        return contentText;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public JsonNode getPayloadJson() {
        return payloadJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setContentText(String contentText) {
        this.contentText = contentText;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public void setPayloadJson(JsonNode payloadJson) {
        this.payloadJson = payloadJson;
    }
}
