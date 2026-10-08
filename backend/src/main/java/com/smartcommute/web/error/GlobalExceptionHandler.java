package com.smartcommute.web.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Central exception -> HTTP mapping (docs/04-lld.md section 6.1, docs/06-api-design.md).
 *
 * <p>Every response includes the correlation id (from MDC, set by the CorrelationIdFilter) and a
 * stable machine-readable code. Unexpected errors return 500 with the correlation id but never leak
 * stack traces or internal messages to the client (NFR-12).</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        ApiError body = new ApiError("VALIDATION_ERROR", "request validation failed", correlationId(), fields);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
        return respond(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
        return respond(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex) {
        return respond(HttpStatus.FORBIDDEN, ex);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return respond(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return respond(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(RateLimitedException.class)
    public ResponseEntity<ApiError> handleRateLimited(RateLimitedException ex) {
        return respond(HttpStatus.TOO_MANY_REQUESTS, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        ApiError body = new ApiError("BAD_REQUEST", ex.getMessage(), correlationId(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        String cid = correlationId();
        // Log the full detail server-side; return a safe generic message to the client.
        log.error("Unhandled exception [reqId={}]", cid, ex);
        ApiError body = new ApiError("INTERNAL_ERROR", "an unexpected error occurred", cid, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private ResponseEntity<ApiError> respond(HttpStatus status, ApiException ex) {
        ApiError body = ApiError.of(ex.getCode(), ex.getMessage(), correlationId());
        return ResponseEntity.status(status).body(body);
    }

    private String correlationId() {
        return MDC.get("requestId");
    }
}
