package com.syncscore.v2.repo;

import com.syncscore.v2.domain.ArchScanStructuralSignal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArchScanStructuralSignalRepository extends JpaRepository<ArchScanStructuralSignal, UUID> {
    List<ArchScanStructuralSignal> findByArchitectureScanId(UUID architectureScanId);
}
