package com.syncscore.v1.repo;

import com.syncscore.v1.domain.ScoreResult;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScoreResultRepository extends JpaRepository<ScoreResult, UUID> {
    Optional<ScoreResult> findByScanEventId(UUID scanEventId);
}

