package com.syncscore.config;

import com.syncscore.auth.service.LoggingOtpSender;
import com.syncscore.auth.service.OtpSender;
import com.syncscore.auth.service.SmtpOtpSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;

@Configuration
public class OtpSenderConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.mail.host")
    public OtpSender smtpOtpSender(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String from
    ) {
        return new SmtpOtpSender(mailSender, from);
    }

    @Bean
    @ConditionalOnMissingBean(OtpSender.class)
    public OtpSender loggingOtpSender() {
        return new LoggingOtpSender();
    }
}