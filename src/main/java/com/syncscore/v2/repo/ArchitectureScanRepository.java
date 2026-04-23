package com.syncscore.v2.repo;

import com.syncscore.v2.domain.ArchitectureScan;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchitectureScanRepository extends JpaRepository<ArchitectureScan, UUID> {
    Optional<ArchitectureScan> findTopByAgencyIdOrderByCreatedAtDesc(UUID agencyId);
    Optional<ArchitectureScan> findTopByAgencyIdAndStatusOrderByCreatedAtDesc(UUID agencyId, String status);
}