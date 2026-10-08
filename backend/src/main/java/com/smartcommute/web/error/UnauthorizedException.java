package com.smartcommute.web.error;

/** Maps to HTTP 401. Authentication is required or credentials are invalid. */
public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }
}
