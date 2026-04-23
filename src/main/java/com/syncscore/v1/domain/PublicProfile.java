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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "public_profiles")
public class PublicProfile {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "agency_id", nullable = false, unique = true)
    private UUID agencyId;

    @Column(nullable = false, unique = true, length = 250)
    private String slug;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_label", nullable = false, length = 30)
    private VerificationLabel verificationLabel;

    @Column(name = "latest_score_result_id")
    private UUID latestScoreResultId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PublicProfile() {}

    public PublicProfile(UUID agencyId, String slug, VerificationLabel verificationLabel) {
        this.agencyId = agencyId;
        this.slug = slug;
        this.verificationLabel = verificationLabel;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAgencyId() {
        return agencyId;
    }

    public String getSlug() {
        return slug;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public VerificationLabel getVerificationLabel() {
        return verificationLabel;
    }

    public UUID getLatestScoreResultId() {
        return latestScoreResultId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setLatestScoreResultId(UUID latestScoreResultId) {
        this.latestScoreResultId = latestScoreResultId;
    }

    public void setVerificationLabel(VerificationLabel verificationLabel) {
        this.verificationLabel = verificationLabel;
    }

    public void setSlugIfUnpublished(String slug) {
        if (this.publishedAt != null) {
            return;
        }
        this.slug = slug;
    }

    public void publishNow(Instant now) {
        this.isPublic = true;
        if (this.publishedAt == null) {
            this.publishedAt = now;
        }
    }

    public void unpublish() {
        this.isPublic = false;
    }
}
