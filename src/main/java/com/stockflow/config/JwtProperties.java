package com.stockflow.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stockflow.security.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, String issuer) {
}