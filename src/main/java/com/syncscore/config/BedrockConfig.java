package com.syncscore.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

@Configuration
@EnableConfigurationProperties(BedrockProperties.class)
public class BedrockConfig {

    @Bean
    public BedrockRuntimeClient bedrockRuntimeClient(BedrockProperties props) {
        return BedrockRuntimeClient.builder()
                .region(Region.of(props.region()))
                .build();
    }
}