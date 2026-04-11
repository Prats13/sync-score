package com.syncscore.auth.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(nullable = true, unique = true, length = 50)
    private String username;

    @Column(nullable = true, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserAccountStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(nullable = false)
    private Instant createdAt;

    protected UserEntity() {}

    public UserEntity(String email) {
        this.email = email.toLowerCase().trim();
        this.status = UserAccountStatus.PENDING_PROFILE;
        this.roles.add(Role.USER);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserAccountStatus getStatus() {
        return status;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void completeProfile(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.status = UserAccountStatus.ACTIVE;
    }
}

