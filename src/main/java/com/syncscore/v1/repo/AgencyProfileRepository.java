package com.syncscore.v1.repo;

import com.syncscore.v1.domain.AgencyProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgencyProfileRepository extends JpaRepository<AgencyProfile, UUID> {
    Optional<AgencyProfile> findByUserId(UUID userId);
}

