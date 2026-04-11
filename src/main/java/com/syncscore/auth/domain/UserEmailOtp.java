package com.syncscore.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "user_email_otps")
public class UserEmailOtp {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(nullable = false, length = 100)
    private String otpHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = true)
    private Instant consumedAt;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant createdAt;

    protected UserEmailOtp() {}

    public UserEmailOtp(UUID userId, OtpPurpose purpose, String otpHash, Instant expiresAt) {
        this.userId = userId;
        this.purpose = purpose;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public OtpPurpose getPurpose() {
        return purpose;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public void consume(Instant now) {
        this.consumedAt = now;
    }
}
