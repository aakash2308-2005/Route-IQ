package com.smartcommute.web.error;

/** Maps to HTTP 400. Invalid input that Bean Validation did not catch (e.g. unresolvable place). */
public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super("BAD_REQUEST", message);
    }
}
