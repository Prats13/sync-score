package com.syncscore.v1.repo;

import com.syncscore.v1.domain.RepositoryScan;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryScanRepository extends JpaRepository<RepositoryScan, UUID> {
    List<RepositoryScan> findByScanEventId(UUID scanEventId);
}

