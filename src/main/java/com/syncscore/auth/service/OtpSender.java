package com.syncscore.auth.service;

public interface OtpSender {
    void sendSignupOtp(String email, String otp);
}

