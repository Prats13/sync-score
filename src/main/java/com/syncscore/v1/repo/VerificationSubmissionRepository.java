package com.syncscore.v1.repo;

import com.syncscore.v1.domain.VerificationSubmission;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationSubmissionRepository extends JpaRepository<VerificationSubmission, UUID> {
    List<VerificationSubmission> findByAgencyIdOrderByCreatedAtDesc(UUID agencyId);
    Optional<VerificationSubmission> findTopByAgencyIdOrderByCreatedAtDesc(UUID agencyId);
}
