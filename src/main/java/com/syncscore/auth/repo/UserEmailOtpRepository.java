package com.syncscore.auth.repo;

import com.syncscore.auth.domain.OtpPurpose;
import com.syncscore.auth.domain.UserEmailOtp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserEmailOtpRepository extends JpaRepository<UserEmailOtp, UUID> {
    Optional<UserEmailOtp> findTopByUserIdAndPurposeOrderByCreatedAtDesc(UUID userId, OtpPurpose purpose);
}

