package com.syncscore.auth.service;

import com.syncscore.auth.domain.OtpPurpose;
import com.syncscore.auth.domain.UserEmailOtp;
import com.syncscore.auth.repo.UserEmailOtpRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserEmailOtpService {
    private final UserEmailOtpRepository otpRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpSender otpSender;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    private final Duration signupOtpTtl;
    private final Duration resendCooldown;

    public UserEmailOtpService(
            UserEmailOtpRepository otpRepository,
            PasswordEncoder passwordEncoder,
            OtpSender otpSender,
            Clock clock,
            @Value("${app.otp.signup-ttl-seconds:600}") long signupOtpTtlSeconds,
            @Value("${app.otp.resend-cooldown-seconds:60}") long resendCooldownSeconds
    ) {
        this.otpRepository = otpRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpSender = otpSender;
        this.clock = clock;
        this.signupOtpTtl = Duration.ofSeconds(signupOtpTtlSeconds);
        this.resendCooldown = Duration.ofSeconds(resendCooldownSeconds);
    }

    @Transactional
    public void issueSignupOtp(UUID userId, String email) {
        Instant now = Instant.now(clock);
        otpRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(userId, OtpPurpose.SIGNUP).ifPresent(latest -> {
            Instant created = latest.getCreatedAt();
            if (created != null && created.plus(resendCooldown).isAfter(now)) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "OTP recently sent. Try again soon.");
            }
        });

        String otp = generateOtp();
        String otpHash = passwordEncoder.encode(otp);
        Instant expiresAt = now.plus(signupOtpTtl);
        otpRepository.save(new UserEmailOtp(userId, OtpPurpose.SIGNUP, otpHash, expiresAt));
        otpSender.sendSignupOtp(email, otp);
    }

    @Transactional
    public boolean verifySignupOtp(UUID userId, String otp) {
        Instant now = Instant.now(clock);
        UserEmailOtp latest = otpRepository.findTopByUserIdAndPurposeOrderByCreatedAtDesc(userId, OtpPurpose.SIGNUP)
                .orElse(null);
        if (latest == null) {
            return false;
        }
        if (latest.getConsumedAt() != null || latest.getExpiresAt().isBefore(now)) {
            return false;
        }
        if (!passwordEncoder.matches(otp, latest.getOtpHash())) {
            return false;
        }
        latest.consume(now);
        otpRepository.save(latest);
        return true;
    }

    private String generateOtp() {
        int value = random.nextInt(1_000_000);
        return String.format("%06d", value);
    }
}
