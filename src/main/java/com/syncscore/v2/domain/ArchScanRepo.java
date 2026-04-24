package com.syncscore.v2.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "arch_scan_repos")
public class ArchScanRepo {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "architecture_scan_id", nullable = false)
    private UUID architectureScanId;

    @Column(name = "repo_full_name", nullable = false, length = 300)
    private String repoFullName;

    @Column(name = "commits_30d", nullable = false)
    private int commits30d;

    @Column(name = "commits_90d", nullable = false)
    private int commits90d;

    @Column(name = "contributor_count", nullable = false)
    private int contributorCount;

    @Column(name = "max_folder_depth", nullable = false)
    private int maxFolderDepth;

    @Column(name = "service_count", nullable = false)
    private int serviceCount;

    @Column(name = "source_file_count", nullable = false)
    private int sourceFileCount;

    @Column(name = "repo_age_months", nullable = false)
    private int repoAgeMonths;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ArchScanRepo() {}

    public ArchScanRepo(UUID architectureScanId, String repoFullName,
                        int commits30d, int commits90d, int contributorCount,
                        int maxFolderDepth, int serviceCount, int sourceFileCount, int repoAgeMonths) {
        this.architectureScanId = architectureScanId;
        this.repoFullName = repoFullName;
        this.commits30d = commits30d;
        this.commits90d = commits90d;
        this.contributorCount = contributorCount;
        this.maxFolderDepth = maxFolderDepth;
        this.serviceCount = serviceCount;
        this.sourceFileCount = sourceFileCount;
        this.repoAgeMonths = repoAgeMonths;
    }

    public UUID getId() { return id; }
    public UUID getArchitectureScanId() { return architectureScanId; }
    public String getRepoFullName() { return repoFullName; }
    public int getCommits30d() { return commits30d; }
    public int getCommits90d() { return commits90d; }
    public int getContributorCount() { return contributorCount; }
    public int getMaxFolderDepth() { return maxFolderDepth; }
    public int getServiceCount() { return serviceCount; }
    public int getSourceFileCount() { return sourceFileCount; }
    public int getRepoAgeMonths() { return repoAgeMonths; }
    public Instant getCreatedAt() { return createdAt; }
}