package com.syncscore.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenHasher {
    private final String pepper;

    public TokenHasher(@Value("${app.refresh.pepper}") String pepper) {
        this.pepper = pepper;
    }

    public String hash(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(pepper.getBytes(StandardCharsets.UTF_8));
            byte[] out = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash refresh token", e);
        }
    }
}

