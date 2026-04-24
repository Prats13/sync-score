package com.syncscore.v2.repo;

import com.syncscore.v2.domain.ArchScanRepo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchScanRepoRepository extends JpaRepository<ArchScanRepo, UUID> {
    List<ArchScanRepo> findByArchitectureScanId(UUID architectureScanId);
}