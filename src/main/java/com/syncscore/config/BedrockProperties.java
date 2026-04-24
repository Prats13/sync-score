package com.syncscore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bedrock")
public record BedrockProperties(String region, String modelId) {}