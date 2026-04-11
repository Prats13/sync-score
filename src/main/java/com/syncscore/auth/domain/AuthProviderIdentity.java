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
@Table(name = "auth_provider_identities")
public class AuthProviderIdentity {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(nullable = false, length = 200)
    private String providerSubject;

    @Column(nullable = false, length = 320)
    private String email;

    @CreationTimestamp
    @Column(nullable = false)
    private Instant createdAt;

    protected AuthProviderIdentity() {}

    public AuthProviderIdentity(UUID userId, AuthProvider provider, String providerSubject, String email) {
        this.userId = userId;
        this.provider = provider;
        this.providerSubject = providerSubject;
        this.email = email;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

    public String getEmail() {
        return email;
    }
}

