package com.smartcommute.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies JSON Web Tokens (docs/04-lld.md section 7).
 *
 * <p>Tokens are stateless: they carry the user id (subject) and an expiry, signed with an HMAC key
 * derived from the configured secret. No server-side session is stored, which keeps the API
 * horizontally scalable (NFR-5).</p>
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirySeconds;

    public JwtService(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirySeconds = properties.expirySeconds();
    }

    /** Issue a signed token for a user. */
    public String issue(long userId, String email) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirySeconds)))
                .signWith(key)
                .compact();
    }

    /** Verify a token's signature and expiry, returning its claims. Throws if invalid/expired. */
    public Claims verify(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long expirySeconds() {
        return expirySeconds;
    }
}
