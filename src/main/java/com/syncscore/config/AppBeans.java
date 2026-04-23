package com.syncscore.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppBeans {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

