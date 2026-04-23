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
@Table(name = "agency_profiles")
public class AgencyProfile {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String niche;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "booking_url", length = 500)
    private String bookingUrl;

    @Column(name = "github_username", length = 200)
    private String githubUsername;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Column(name = "rescan_count", nullable = false)
    private int rescanCount = 0;

    @Column(name = "rescan_limit", nullable = false)
    private int rescanLimit = 5;

    @Column(name = "repo_scan_limit", nullable = false)
    private int repoScanLimit = 10;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AgencyProfile() {}

    public AgencyProfile(UUID userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getNiche() {
        return niche;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getBookingUrl() {
        return bookingUrl;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public int getRescanCount() {
        return rescanCount;
    }

    public int getRescanLimit() {
        return rescanLimit;
    }

    public int getRepoScanLimit() {
        return repoScanLimit;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateProfile(
            String name,
            String niche,
            String websiteUrl,
            String description,
            String bookingUrl,
            String githubUsername,
            boolean isPublic
    ) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        this.niche = niche;
        this.websiteUrl = websiteUrl;
        this.description = description;
        this.bookingUrl = bookingUrl;
        this.githubUsername = githubUsername;
        this.isPublic = isPublic;
    }

    public void setRepoScanLimit(int repoScanLimit) {
        this.repoScanLimit = repoScanLimit;
    }

    public void incrementRescanCountOrThrow() {
        if (rescanCount >= rescanLimit) {
            throw new IllegalStateException("Rescan limit reached");
        }
        this.rescanCount += 1;
    }
}
