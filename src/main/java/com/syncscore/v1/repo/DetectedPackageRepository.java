package com.syncscore.v1.repo;

import com.syncscore.v1.domain.DetectedPackage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectedPackageRepository extends JpaRepository<DetectedPackage, UUID> {
    List<DetectedPackage> findByScanEventId(UUID scanEventId);
}

