package com.smartcommute.web.error;

/** Maps to HTTP 403. The caller is authenticated but not allowed to access the resource (NFR-14). */
public class ForbiddenException extends ApiException {
    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }
}
