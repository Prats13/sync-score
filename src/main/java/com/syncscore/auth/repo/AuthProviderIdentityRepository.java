package com.syncscore.auth.repo;

import com.syncscore.auth.domain.AuthProvider;
import com.syncscore.auth.domain.AuthProviderIdentity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthProviderIdentityRepository extends JpaRepository<AuthProviderIdentity, UUID> {
    Optional<AuthProviderIdentity> findByProviderAndProviderSubject(AuthProvider provider, String providerSubject);
}

