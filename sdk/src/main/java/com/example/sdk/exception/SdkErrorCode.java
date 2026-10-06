package com.example.sdk.exception;

/**
 * Machine-readable error codes shared with the IAS Auth Center contract.
 *
 * <p>These codes mirror the frozen {@code ErrorCode} contract (wp-0). The SDK
 * uses the subset relevant to client-side operations.</p>
 */
public enum SdkErrorCode {

    SUCCESS("SUCCESS"),
    WARNING("WARNING"),
    AUTH_001("AUTH_001"),
    AUTH_002("AUTH_002"),
    USER_001("USER_001"),
    USER_003("USER_003"),
    USER_004("USER_004"),
    LICENSE_001("LICENSE_001"),
    LICENSE_002("LICENSE_002"),
    LICENSE_003("LICENSE_003"),
    LICENSE_004("LICENSE_004"),
    LICENSE_005("LICENSE_005"),
    LICENSE_006("LICENSE_006"),
    INSTANCE_001("INSTANCE_001"),
    INSTANCE_002("INSTANCE_002"),
    INSTANCE_003("INSTANCE_003"),
    INSTANCE_004("INSTANCE_004"),
    PARAM_001("PARAM_001"),
    SYS_001("SYS_001");

    private final String code;

    SdkErrorCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    @Override
    public String toString() {
        return code;
    }
}