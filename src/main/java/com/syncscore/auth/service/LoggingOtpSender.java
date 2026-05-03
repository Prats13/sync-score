package com.syncscore.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingOtpSender implements OtpSender {
    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void sendSignupOtp(String email, String otp) {
        log.info("[DEV] OTP for {}: {}", email, otp);
    }
}

