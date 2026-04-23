package com.syncscore.v1.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "verification_submissions")
public class VerificationSubmission {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "agency_id", nullable = false)
    private UUID agencyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private VerificationSourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected VerificationSubmission() {}

    public VerificationSubmission(UUID agencyId, VerificationSourceType sourceType, SubmissionStatus status) {
        this.agencyId = agencyId;
        this.sourceType = sourceType;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAgencyId() {
        return agencyId;
    }

    public VerificationSourceType getSourceType() {
        return sourceType;
    }

    public SubmissionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void markProcessed() {
        this.status = SubmissionStatus.PROCESSED;
    }

    public void markFailed() {
        this.status = SubmissionStatus.FAILED;
    }
}
