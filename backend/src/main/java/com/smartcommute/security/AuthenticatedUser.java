package com.smartcommute.security;

/** The authenticated principal stored in the security context after JWT verification. */
public record AuthenticatedUser(long userId, String email) {
}
