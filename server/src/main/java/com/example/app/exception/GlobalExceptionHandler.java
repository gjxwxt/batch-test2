package com.example.app.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of("RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthException(AuthException ex) {
        ErrorCode code = ex.getErrorCode();
        HttpStatus status = httpStatusForErrorCode(code);
        log.warn("Auth error [{}]: {}", code.getCode(), ex.getMessage());
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(code.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleJwtAuthentication(JwtAuthenticationException ex) {
        log.warn("JWT authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.of(ErrorCode.AUTH_002.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(LicenseValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleLicenseValidation(LicenseValidationException ex) {
        log.warn("License validation failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(ErrorCode.LICENSE_002.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        List<String> details = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        String message = String.format("Validation failed for %d fields", details.size());
        log.warn("Validation error: {}", details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("VALIDATION_FAILED", message, details));
    }

    @ExceptionHandler(LicenseException.class)
    public ResponseEntity<ApiErrorResponse> handleLicenseException(LicenseException ex) {
        ErrorCode code = ex.getErrorCode();
        HttpStatus status = httpStatusForErrorCode(code);
        log.warn("License error [{}]: {}", code.getCode(), ex.getMessage());
        return ResponseEntity.status(status)
                .body(ApiErrorResponse.of(code.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of("BAD_REQUEST", ex.getMessage()));
    }

    /**
     * 依据共享契约错误语义（infra:error-codes）映射 HTTP 状态码。
     */
    private HttpStatus httpStatusForErrorCode(ErrorCode code) {
        return switch (code) {
            case AUTH_001, AUTH_002 -> HttpStatus.UNAUTHORIZED;
            case LICENSE_001, USER_001, INSTANCE_002 -> HttpStatus.NOT_FOUND;
            case LICENSE_006 -> HttpStatus.CONFLICT;
            case LICENSE_002, LICENSE_003, LICENSE_004, LICENSE_005,
                 USER_003, USER_004, PARAM_001 -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled internal server error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiErrorResponse.of("INTERNAL_SERVER_ERROR", "An unexpected internal error occurred"));
    }
}
