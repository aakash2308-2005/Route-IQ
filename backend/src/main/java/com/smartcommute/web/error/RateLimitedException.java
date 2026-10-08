package com.smartcommute.web.error;

/** Maps to HTTP 429. The caller exceeded the allowed request rate (NFR-13). */
public class RateLimitedException extends ApiException {
    public RateLimitedException(String message) {
        super("RATE_LIMITED", message);
    }
}
