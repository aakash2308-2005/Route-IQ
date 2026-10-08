package com.smartcommute.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request/response DTOs for authentication (docs/06-api-design.md section 2). */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @Size(max = 100) String displayName) {
    }

    public record RegisterResponse(long id, String email, String displayName) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {
    }

    public record LoginResponse(String token, long expiresInSeconds, long userId) {
    }
}
