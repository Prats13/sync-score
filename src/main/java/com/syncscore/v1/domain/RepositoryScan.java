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
@Table(name = "repository_scans")
public class RepositoryScan {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "scan_event_id", nullable = false)
    private UUID scanEventId;

    @Column(name = "repo_full_name", nullable = false, length = 400)
    private String repoFullName;

    @Column(name = "repo_url", nullable = false, length = 1000)
    private String repoUrl;

    @Column(name = "default_branch", length = 200)
    private String defaultBranch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RepositoryScanStatus status;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "manifests_found")
    private JsonNode manifestsFound;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RepositoryScan() {}

    public RepositoryScan(UUID scanEventId,
                          String repoFullName,
                          String repoUrl,
                          RepositoryScanStatus status) {
        this.scanEventId = scanEventId;
        this.repoFullName = repoFullName;
        this.repoUrl = repoUrl;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getScanEventId() {
        return scanEventId;
    }

    public String getRepoFullName() {
        return repoFullName;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public String getDefaultBranch() {
        return defaultBranch;
    }

    public RepositoryScanStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public JsonNode getManifestsFound() {
        return manifestsFound;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }

    public void setManifestsFound(JsonNode manifestsFound) {
        this.manifestsFound = manifestsFound;
    }

    public void markScanned() {
        this.status = RepositoryScanStatus.SCANNED;
        this.errorMessage = null;
    }

    public void markSkipped() {
        this.status = RepositoryScanStatus.SKIPPED;
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = RepositoryScanStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}
