package com.example.sdk.exception;

/**
 * Base unchecked exception thrown by the IAS Auth Center client SDK.
 *
 * <p>Carries a stable {@code code} aligned with the shared error-code contract
 * (e.g. {@code LICENSE_001}, {@code PARAM_001}, {@code SYS_001}) so embedded
 * callers can branch on machine-readable codes rather than message text.</p>
 */
public class SdkException extends RuntimeException {

    private final String code;

    public SdkException(String code, String message) {
        super(message);
        this.code = code;
    }

    public SdkException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}