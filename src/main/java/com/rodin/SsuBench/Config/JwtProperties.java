package com.rodin.SsuBench.Config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        Long accessExpiration,
        Long refreshExpiration
) {
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret cannot be empty");
        }
        if (accessExpiration == null || accessExpiration <= 0) {
            throw new IllegalArgumentException("JWT access expiration must be positive");
        }
        if (refreshExpiration == null || refreshExpiration <= 0) {
            throw new IllegalArgumentException("JWT refresh expiration must be positive");
        }
    }
}
