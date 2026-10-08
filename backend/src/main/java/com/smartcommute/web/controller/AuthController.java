package com.smartcommute.web.controller;

import com.smartcommute.application.user.UserService;
import com.smartcommute.observability.RateLimiter;
import com.smartcommute.persistence.entity.UserEntity;
import com.smartcommute.web.dto.AuthDtos;
import com.smartcommute.web.error.RateLimitedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Authentication endpoints (docs/06-api-design.md section 2). Login is rate-limited per client IP to
 * slow credential-stuffing (NFR-13).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;
    private final RateLimiter rateLimiter;

    public AuthController(UserService userService, RateLimiter rateLimiter) {
        this.userService = userService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDtos.RegisterResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest req) {
        UserEntity user = userService.register(req.email(), req.password(), req.displayName());
        AuthDtos.RegisterResponse body =
                new AuthDtos.RegisterResponse(user.getId(), user.getEmail(), user.getDisplayName());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDtos.LoginResponse> login(@Valid @RequestBody AuthDtos.LoginRequest req,
                                                        HttpServletRequest servletRequest) {
        String clientKey = "login:" + clientIp(servletRequest);
        if (!rateLimiter.allow(clientKey, 10, Duration.ofSeconds(60))) {
            throw new RateLimitedException("too many login attempts; try again shortly");
        }
        UserService.AuthResult result = userService.authenticate(req.email(), req.password());
        return ResponseEntity.ok(
                new AuthDtos.LoginResponse(result.token(), result.expiresInSeconds(), result.userId()));
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
