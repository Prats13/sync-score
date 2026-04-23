package com.syncscore.v1.repo;

import com.syncscore.v1.domain.ScanEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanEventRepository extends JpaRepository<ScanEvent, UUID> {
    List<ScanEvent> findByAgencyIdOrderByCreatedAtDesc(UUID agencyId);
}

