package com.syncscore.config;

import com.syncscore.auth.service.LoggingOtpSender;
import com.syncscore.auth.service.OtpSender;
import com.syncscore.auth.service.ResendOtpSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OtpSenderConfig {

    @Bean
    @ConditionalOnProperty(name = "app.resend.api-key")
    public OtpSender resendOtpSender(
            @Value("${app.resend.api-key}") String apiKey,
            @Value("${app.mail.from}") String from
    ) {
        return new ResendOtpSender(apiKey, from);
    }

    @Bean
    @ConditionalOnMissingBean(OtpSender.class)
    public OtpSender loggingOtpSender() {
        return new LoggingOtpSender();
    }
}
