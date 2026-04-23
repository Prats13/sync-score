package com.syncscore.producer.domain;

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
@Table(name = "producer_profiles")
public class ProducerProfile {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "linkedin_url", nullable = false, length = 500)
    private String linkedinUrl;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @Column(name = "website_url", nullable = false, length = 500)
    private String websiteUrl;

    @Column(name = "live_project_url", nullable = false, length = 500)
    private String liveProjectUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge2_status", nullable = false, length = 30)
    private Badge2Status badge2Status = Badge2Status.PENDING;

    @Column(name = "linkedin_reachable")
    private Boolean linkedinReachable;

    @Column(name = "github_reachable")
    private Boolean githubReachable;

    @Column(name = "website_reachable")
    private Boolean websiteReachable;

    @Column(name = "live_project_reachable")
    private Boolean liveProjectReachable;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProducerProfile() {}

    public ProducerProfile(UUID userId, String linkedinUrl, String githubUrl,
                           String websiteUrl, String liveProjectUrl, Instant now) {
        this.userId = userId;
        this.linkedinUrl = linkedinUrl;
        this.githubUrl = githubUrl;
        this.websiteUrl = websiteUrl;
        this.liveProjectUrl = liveProjectUrl;
        this.badge2Status = Badge2Status.PENDING;
        this.updatedAt = now;
    }

    public void updateUrls(String linkedinUrl, String githubUrl,
                           String websiteUrl, String liveProjectUrl, Instant now) {
        this.linkedinUrl = linkedinUrl;
        this.githubUrl = githubUrl;
        this.websiteUrl = websiteUrl;
        this.liveProjectUrl = liveProjectUrl;
        this.badge2Status = Badge2Status.PENDING;
        this.linkedinReachable = null;
        this.githubReachable = null;
        this.websiteReachable = null;
        this.liveProjectReachable = null;
        this.verifiedAt = null;
        this.updatedAt = now;
    }

    public void recordVerification(boolean linkedinReachable, Boolean githubReachable,
                                   boolean websiteReachable, boolean liveProjectReachable,
                                   Badge2Status status, Instant now) {
        this.linkedinReachable = linkedinReachable;
        this.githubReachable = githubReachable;
        this.websiteReachable = websiteReachable;
        this.liveProjectReachable = liveProjectReachable;
        this.badge2Status = status;
        this.verifiedAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public String getGithubUrl() { return githubUrl; }
    public String getWebsiteUrl() { return websiteUrl; }
    public String getLiveProjectUrl() { return liveProjectUrl; }
    public Badge2Status getBadge2Status() { return badge2Status; }
    public Boolean getLinkedinReachable() { return linkedinReachable; }
    public Boolean getGithubReachable() { return githubReachable; }
    public Boolean getWebsiteReachable() { return websiteReachable; }
    public Boolean getLiveProjectReachable() { return liveProjectReachable; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getCreatedAt() { return createdAt; }
}