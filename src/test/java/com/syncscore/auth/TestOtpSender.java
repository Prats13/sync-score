package com.syncscore.auth;

import com.syncscore.auth.service.OtpSender;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestOtpSender implements OtpSender {
    private final Map<String, String> lastOtpByEmail = new ConcurrentHashMap<>();

    @Override
    public void sendSignupOtp(String email, String otp) {
        lastOtpByEmail.put(email.toLowerCase().trim(), otp);
    }

    public String lastOtpFor(String email) {
        return lastOtpByEmail.get(email.toLowerCase().trim());
    }
}

