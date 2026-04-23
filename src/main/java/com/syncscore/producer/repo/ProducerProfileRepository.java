package com.syncscore.producer.repo;

import com.syncscore.producer.domain.ProducerProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProducerProfileRepository extends JpaRepository<ProducerProfile, UUID> {
    Optional<ProducerProfile> findByUserId(UUID userId);
}