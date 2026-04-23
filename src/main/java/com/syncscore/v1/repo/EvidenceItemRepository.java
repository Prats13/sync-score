package com.syncscore.v1.repo;

import com.syncscore.v1.domain.EvidenceItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvidenceItemRepository extends JpaRepository<EvidenceItem, UUID> {
    List<EvidenceItem> findByAgencyIdOrderByCreatedAtDesc(UUID agencyId);
}

