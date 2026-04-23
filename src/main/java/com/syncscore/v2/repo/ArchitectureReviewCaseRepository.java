package com.syncscore.v2.repo;

import com.syncscore.v2.domain.ArchitectureReviewCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchitectureReviewCaseRepository extends JpaRepository<ArchitectureReviewCase, UUID> {
    Optional<ArchitectureReviewCase> findTopByAgencyIdAndStatusOrderByCreatedAtDesc(UUID agencyId, String status);
    List<ArchitectureReviewCase> findByStatusOrderByCreatedAtDesc(String status);
    List<ArchitectureReviewCase> findByAgencyIdOrderByCreatedAtDesc(UUID agencyId);
}