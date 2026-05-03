package com.syncscore.auth.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

public class SmtpOtpSender implements OtpSender {
    private final JavaMailSender mailSender;
    private final String from;

    public SmtpOtpSender(JavaMailSender mailSender, String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendSignupOtp(String email, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(email);
        msg.setSubject("Your SyncScore verification code");
        msg.setText("Your verification code is: " + otp + "\n\nThis code expires in 10 minutes.");
        mailSender.send(msg);
    }
}