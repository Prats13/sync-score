package com.syncscore.security;

import com.syncscore.auth.domain.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {
    private final Key key;
    private final String issuer;

    public JwtService(
            @Value("${app.jwt.secret-base64}") String secretBase64,
            @Value("${app.jwt.issuer:sync-score}") String issuer
    ) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.issuer = issuer;
    }

    public String issueAccessToken(UUID userId, String username, Set<Role> roles, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim("type", "access")
                .claim("username", username)
                .claim("roles", roles.stream().map(Enum::name).toList())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public String issueSignupToken(UUID userId, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .claim("type", "signup")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public String issueOpaqueRefreshToken() {
        return java.util.UUID.randomUUID().toString() + "." + java.util.UUID.randomUUID().toString();
    }

    public AccessPrincipal tryParseAccessToken(String authorizationHeader) {
        try {
            String token = authorizationHeader.substring("Bearer ".length());
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
            Claims claims = jws.getPayload();
            if (!"access".equals(claims.get("type", String.class))) {
                return null;
            }
            UUID userId = UUID.fromString(claims.getSubject());
            String username = claims.get("username", String.class);
            List<String> roles = claims.get("roles", List.class);
            Set<Role> parsedRoles = roles == null
                    ? Set.of()
                    : roles.stream().map(Role::valueOf).collect(java.util.stream.Collectors.toSet());
            return new AccessPrincipal(userId, username, parsedRoles);
        } catch (Exception ignored) {
            return null;
        }
    }

    public UUID parseAndValidateSignupToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Missing signup token"
            );
        }
        try {
            String token = authorizationHeader.substring("Bearer ".length());
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith((javax.crypto.SecretKey) key)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
            Claims claims = jws.getPayload();
            if (!"signup".equals(claims.get("type", String.class))) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED,
                        "Invalid signup token"
                );
            }
            return UUID.fromString(claims.getSubject());
        } catch (org.springframework.web.server.ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Invalid signup token"
            );
        }
    }
}

