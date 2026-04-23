package com.syncscore.auth.service;

import com.syncscore.auth.domain.AuthProvider;
import com.syncscore.auth.domain.AuthProviderIdentity;
import com.syncscore.auth.domain.RefreshToken;
import com.syncscore.auth.domain.UserAccountStatus;
import com.syncscore.auth.domain.UserEntity;
import com.syncscore.auth.repo.AuthProviderIdentityRepository;
import com.syncscore.auth.repo.RefreshTokenRepository;
import com.syncscore.auth.repo.UserRepository;
import com.syncscore.auth.service.dto.GoogleLoginRequest;
import com.syncscore.auth.service.dto.EmailStatusRequest;
import com.syncscore.auth.service.dto.EmailStatusResponse;
import com.syncscore.auth.service.dto.LoginRequest;
import com.syncscore.auth.service.dto.LogoutRequest;
import com.syncscore.auth.service.dto.RefreshRequest;
import com.syncscore.auth.service.dto.SignupCompleteRequest;
import com.syncscore.auth.service.dto.SignupStartEmailRequest;
import com.syncscore.auth.service.dto.SignupVerifyOtpRequest;
import com.syncscore.auth.service.dto.TokenPairResponse;
import com.syncscore.auth.service.dto.TokenResponse;
import com.syncscore.security.JwtService;
import com.syncscore.security.TokenHasher;
import com.syncscore.security.google.GoogleIdTokenPayload;
import com.syncscore.security.google.GoogleTokenVerifier;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultAuthService implements AuthService {
    private final UserRepository userRepository;
    private final AuthProviderIdentityRepository identityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenHasher tokenHasher;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserEmailOtpService userEmailOtpService;
    private final Clock clock;

    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final Duration signupTtl;

    public DefaultAuthService(
            UserRepository userRepository,
            AuthProviderIdentityRepository identityRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            TokenHasher tokenHasher,
            GoogleTokenVerifier googleTokenVerifier,
            UserEmailOtpService userEmailOtpService,
            Clock clock
    ) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenHasher = tokenHasher;
        this.googleTokenVerifier = googleTokenVerifier;
        this.userEmailOtpService = userEmailOtpService;
        this.clock = clock;
        this.accessTtl = Duration.ofMinutes(15);
        this.refreshTtl = Duration.ofDays(30);
        this.signupTtl = Duration.ofMinutes(15);
    }

    @Override
    @Transactional
    public void startEmailSignup(SignupStartEmailRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT).trim();
        UserEntity user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(new UserEntity(email)));
        if (user.getStatus() == UserAccountStatus.DISABLED) {
            throw AuthServiceExceptions.disabled();
        }
        if (user.getStatus() == UserAccountStatus.ACTIVE) {
            throw AuthServiceExceptions.emailAlreadyRegistered();
        }
        userEmailOtpService.issueSignupOtp(user.getId(), user.getEmail());
    }

    @Override
    @Transactional
    public void resendEmailSignupOtp(SignupStartEmailRequest request) {
        // Same semantics as start: for signup (pending profile), send OTP with cooldown.
        startEmailSignup(request);
    }

    @Override
    @Transactional
    public TokenResponse verifyEmailSignupOtp(SignupVerifyOtpRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT).trim();
        UserEntity user = userRepository.findByEmail(email).orElseThrow(AuthServiceExceptions::userNotFound);
        if (user.getStatus() == UserAccountStatus.DISABLED) {
            throw AuthServiceExceptions.disabled();
        }
        if (user.getStatus() == UserAccountStatus.ACTIVE) {
            throw AuthServiceExceptions.emailAlreadyRegistered();
        }

        boolean ok = userEmailOtpService.verifySignupOtp(user.getId(), request.otp());
        if (!ok) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Invalid or expired OTP"
            );
        }
        return new TokenResponse(jwtService.issueSignupToken(user.getId(), signupTtl));
    }

    @Override
    @Transactional
    public TokenResponse signupWithGoogle(GoogleLoginRequest request) {
        GoogleIdTokenPayload payload = googleTokenVerifier.verify(request.idToken());
        UserEntity user = upsertGoogleUser(payload);
        if (user.getStatus() == UserAccountStatus.ACTIVE) {
            throw AuthServiceExceptions.emailAlreadyRegistered();
        }
        return new TokenResponse(jwtService.issueSignupToken(user.getId(), signupTtl));
    }

    @Override
    @Transactional
    public TokenPairResponse completeSignup(String authorizationHeader, SignupCompleteRequest request) {
        UUID userId = jwtService.parseAndValidateSignupToken(authorizationHeader);
        UserEntity user = userRepository.findById(userId).orElseThrow(AuthServiceExceptions::userNotFound);
        if (user.getStatus() == UserAccountStatus.DISABLED) {
            throw AuthServiceExceptions.disabled();
        }
        if (user.getStatus() == UserAccountStatus.ACTIVE) {
            throw AuthServiceExceptions.alreadyActive();
        }

        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw AuthServiceExceptions.usernameTaken();
        }

        String passwordHash = passwordEncoder.encode(request.password());
        user.completeProfile(username, passwordHash);
        userRepository.save(user);

        return issueTokenPair(user);
    }

    @Override
    @Transactional(readOnly = true)
    public TokenPairResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByUsername(request.username().trim())
                .orElseThrow(AuthServiceExceptions::invalidCredentials);
        if (user.getStatus() != UserAccountStatus.ACTIVE) {
            throw AuthServiceExceptions.invalidCredentials();
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw AuthServiceExceptions.invalidCredentials();
        }
        return issueTokenPair(user);
    }

    @Override
    @Transactional
    public TokenPairResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleIdTokenPayload payload = googleTokenVerifier.verify(request.idToken());
        UserEntity user = upsertGoogleUser(payload);
        if (user.getStatus() == UserAccountStatus.DISABLED) {
            throw AuthServiceExceptions.disabled();
        }
        if (user.getStatus() != UserAccountStatus.ACTIVE) {
            throw AuthServiceExceptions.signupIncomplete(jwtService.issueSignupToken(user.getId(), signupTtl));
        }
        return issueTokenPair(user);
    }

    @Override
    @Transactional
    public TokenPairResponse refresh(RefreshRequest request) {
        String tokenHash = tokenHasher.hash(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(AuthServiceExceptions::invalidRefreshToken);

        Instant now = Instant.now(clock);
        if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(now)) {
            throw AuthServiceExceptions.invalidRefreshToken();
        }

        stored.revoke(now);
        refreshTokenRepository.save(stored);

        UserEntity user = userRepository.findById(stored.getUserId()).orElseThrow(AuthServiceExceptions::userNotFound);
        if (user.getStatus() != UserAccountStatus.ACTIVE) {
            throw AuthServiceExceptions.invalidRefreshToken();
        }

        return issueTokenPair(user);
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        String tokenHash = tokenHasher.hash(request.refreshToken());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
            rt.revoke(Instant.now(clock));
            refreshTokenRepository.save(rt);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public EmailStatusResponse emailStatus(EmailStatusRequest request) {
        String email = request.email().toLowerCase(Locale.ROOT).trim();
        UserEntity user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return new EmailStatusResponse(false, null);
        }
        return new EmailStatusResponse(true, user.getStatus().name());
    }

    @Override
    public Object authHealth(HttpServletRequest request) {
        return java.util.Map.of(
                "ok", true,
                "path", request.getRequestURI()
        );
    }

    private TokenPairResponse issueTokenPair(UserEntity user) {
        String accessToken = jwtService.issueAccessToken(user.getId(), user.getUsername(), user.getRoles(), accessTtl);
        String refreshToken = jwtService.issueOpaqueRefreshToken();

        String refreshTokenHash = tokenHasher.hash(refreshToken);
        Instant expiresAt = Instant.now(clock).plus(refreshTtl);
        refreshTokenRepository.save(new RefreshToken(user.getId(), refreshTokenHash, expiresAt));

        return new TokenPairResponse(accessToken, refreshToken, "bearer");
    }

    private UserEntity upsertGoogleUser(GoogleIdTokenPayload payload) {
        String email = payload.email().toLowerCase(Locale.ROOT).trim();
        String subject = payload.subject();

        return identityRepository.findByProviderAndProviderSubject(AuthProvider.GOOGLE, subject)
                .flatMap(identity -> userRepository.findById(identity.getUserId()))
                .orElseGet(() -> {
                    UserEntity user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(new UserEntity(email)));
                    identityRepository.save(new AuthProviderIdentity(user.getId(), AuthProvider.GOOGLE, subject, email));
                    return user;
                });
    }
}
