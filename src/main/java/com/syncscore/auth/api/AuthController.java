package com.syncscore.auth.api;

import com.syncscore.auth.service.AuthService;
import com.syncscore.auth.service.dto.EmailStatusRequest;
import com.syncscore.auth.service.dto.EmailStatusResponse;
import com.syncscore.auth.service.dto.GoogleLoginRequest;
import com.syncscore.auth.service.dto.LoginRequest;
import com.syncscore.auth.service.dto.LogoutRequest;
import com.syncscore.auth.service.dto.RefreshRequest;
import com.syncscore.auth.service.dto.SignupCompleteRequest;
import com.syncscore.auth.service.dto.SignupStartEmailRequest;
import com.syncscore.auth.service.dto.SignupVerifyOtpRequest;
import com.syncscore.auth.service.dto.TokenPairResponse;
import com.syncscore.auth.service.dto.TokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup/email/start")
    public void startEmailSignup(@Valid @RequestBody SignupStartEmailRequest request) {
        authService.startEmailSignup(request);
    }

    @PostMapping("/signup/email/resend-otp")
    public void resendEmailOtp(@Valid @RequestBody SignupStartEmailRequest request) {
        authService.resendEmailSignupOtp(request);
    }

    @PostMapping("/signup/email/verify-otp")
    public TokenResponse verifyEmailOtp(@Valid @RequestBody SignupVerifyOtpRequest request) {
        return authService.verifyEmailSignupOtp(request);
    }

    @PostMapping("/email/status")
    public EmailStatusResponse emailStatus(@Valid @RequestBody EmailStatusRequest request) {
        return authService.emailStatus(request);
    }

    @PostMapping("/signup/google")
    public TokenResponse signupWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.signupWithGoogle(request);
    }

    @PostMapping("/signup/complete")
    public TokenPairResponse completeSignup(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody SignupCompleteRequest request
    ) {
        return authService.completeSignup(authorization, request);
    }

    @PostMapping("/login")
    public TokenPairResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/login/google")
    public TokenPairResponse loginWithGoogle(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request);
    }

    @PostMapping("/token/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
    }

    @GetMapping("/health")
    public Object authHealth(HttpServletRequest servletRequest) {
        return authService.authHealth(servletRequest);
    }
}
