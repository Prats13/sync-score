package com.syncscore.v2.repo;

import com.syncscore.v2.domain.ConfidentialScanSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfidentialScanSessionRepository extends JpaRepository<ConfidentialScanSession, UUID> {
    Optional<ConfidentialScanSession> findByArchScanId(UUID archScanId);
}
