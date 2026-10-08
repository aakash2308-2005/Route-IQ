package com.smartcommute.web.error;

/** Maps to HTTP 404. The requested resource does not exist. */
public class NotFoundException extends ApiException {
    public NotFoundException(String message) {
        super("NOT_FOUND", message);
    }
}
