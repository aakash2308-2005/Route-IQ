package com.smartcommute.web.error;

/** Maps to HTTP 409. A uniqueness/state conflict (e.g. duplicate email or place label). */
public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}
