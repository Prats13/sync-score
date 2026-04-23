package com.syncscore.producer.service;

import com.syncscore.producer.service.dto.ProducerProfileRequest;
import com.syncscore.producer.service.dto.ProducerProfileResponse;
import java.util.UUID;

public interface ProducerService {
    ProducerProfileResponse submitProfile(UUID userId, ProducerProfileRequest request);
    ProducerProfileResponse getProfile(UUID userId);
    ProducerProfileResponse verifyProfile(UUID userId);
}