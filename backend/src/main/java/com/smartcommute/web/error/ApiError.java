package com.smartcommute.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Uniform error response body (docs/06-api-design.md section 1). {@code fields} is only present for
 * validation errors. {@code correlationId} lets a user reference a specific failed request without
 * exposing internal details.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String code, String message, String correlationId, Map<String, String> fields) {

    public static ApiError of(String code, String message, String correlationId) {
        return new ApiError(code, message, correlationId, null);
    }
}
