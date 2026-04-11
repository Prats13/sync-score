package com.syncscore.auth.service;

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
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    void startEmailSignup(SignupStartEmailRequest request);
    void resendEmailSignupOtp(SignupStartEmailRequest request);
    TokenResponse verifyEmailSignupOtp(SignupVerifyOtpRequest request);
    TokenResponse signupWithGoogle(GoogleLoginRequest request);
    TokenPairResponse completeSignup(String authorizationHeader, SignupCompleteRequest request);
    TokenPairResponse login(LoginRequest request);
    TokenPairResponse loginWithGoogle(GoogleLoginRequest request);
    TokenPairResponse refresh(RefreshRequest request);
    void logout(LogoutRequest request);
    EmailStatusResponse emailStatus(EmailStatusRequest request);
    Object authHealth(HttpServletRequest request);
}
