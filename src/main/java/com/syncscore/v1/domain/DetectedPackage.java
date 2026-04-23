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
@Table(name = "detected_packages")
public class DetectedPackage {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "scan_event_id", nullable = false)
    private UUID scanEventId;

    @Column(name = "repository_scan_id")
    private UUID repositoryScanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "manifest_type", nullable = false, length = 30)
    private ManifestType manifestType;

    @Column(name = "manifest_path", length = 1000)
    private String manifestPath;

    @Column(name = "package_name_normalized", nullable = false, length = 200)
    private String packageNameNormalized;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "points_awarded", nullable = false)
    private int pointsAwarded;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected DetectedPackage() {}

    public DetectedPackage(UUID scanEventId,
                           UUID repositoryScanId,
                           ManifestType manifestType,
                           String manifestPath,
                           String packageNameNormalized,
                           String category,
                           int pointsAwarded) {
        this.scanEventId = scanEventId;
        this.repositoryScanId = repositoryScanId;
        this.manifestType = manifestType;
        this.manifestPath = manifestPath;
        this.packageNameNormalized = packageNameNormalized;
        this.category = category;
        this.pointsAwarded = pointsAwarded;
    }

    public UUID getId() {
        return id;
    }

    public UUID getScanEventId() {
        return scanEventId;
    }

    public UUID getRepositoryScanId() {
        return repositoryScanId;
    }

    public ManifestType getManifestType() {
        return manifestType;
    }

    public String getManifestPath() {
        return manifestPath;
    }

    public String getPackageNameNormalized() {
        return packageNameNormalized;
    }

    public String getCategory() {
        return category;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
