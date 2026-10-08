package com.smartcommute.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT configuration bound from {@code smartcommute.security.jwt.*}. The secret is supplied via
 * environment variable in real deployments and must never be committed (NFR-11, NFR-12).
 */
@ConfigurationProperties(prefix = "smartcommute.security.jwt")
public record JwtProperties(String secret, long expirySeconds) {

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "JWT secret must be at least 32 characters. Set JWT_SECRET env var.");
        }
        if (expirySeconds <= 0) {
            expirySeconds = 3600;
        }
    }
}
