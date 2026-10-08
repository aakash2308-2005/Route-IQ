package com.smartcommute.web.error;

/**
 * Base class for application exceptions that map to a specific HTTP response. Carries a stable
 * machine-readable {@code code} used in the {@link ApiError} body (docs/06-api-design.md section 1).
 */
public abstract class ApiException extends RuntimeException {

    private final String code;

    protected ApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
