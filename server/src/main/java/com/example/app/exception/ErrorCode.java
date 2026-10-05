package com.example.app.exception;

import org.springframework.http.HttpStatus;

/**
 * 统一业务错误码（共享契约 infra:error-codes）。
 *
 * <p>所有业务失败通过该枚举表达，由 {@link GlobalExceptionHandler} 转换为统一的
 * {@link ApiErrorResponse}。错误码命名遵循契约：{@code SUCCESS}/{@code WARNING}
 * 为通用状态，其余按领域前缀分组（AUTH/USER/LICENSE/INSTANCE/PARAM/SYS）。</p>
 */
public enum ErrorCode {

    // ---- 通用状态 ----
    SUCCESS("SUCCESS", HttpStatus.OK, "操作成功"),
    WARNING("WARNING", HttpStatus.OK, "操作成功但存在警告"),

    // ---- 认证 / 鉴权 (AUTH) ----
    AUTH_001("AUTH_001", HttpStatus.UNAUTHORIZED, "登录失败：用户名或密码错误"),
    AUTH_002("AUTH_002", HttpStatus.UNAUTHORIZED, "令牌无效或已过期"),

    // ---- 管理员账号 (USER) ----
    USER_001("USER_001", HttpStatus.NOT_FOUND, "管理员账号不存在"),
    USER_003("USER_003", HttpStatus.BAD_REQUEST, "管理员账号已禁用"),
    USER_004("USER_004", HttpStatus.BAD_REQUEST, "原密码校验失败"),

    // ---- 授权 (LICENSE) ----
    LICENSE_001("LICENSE_001", HttpStatus.NOT_FOUND, "授权记录不存在"),
    LICENSE_002("LICENSE_002", HttpStatus.BAD_REQUEST, "授权序列号已存在"),
    LICENSE_003("LICENSE_003", HttpStatus.BAD_REQUEST, "授权文件格式非法"),
    LICENSE_004("LICENSE_004", HttpStatus.BAD_REQUEST, "授权签名校验失败"),
    LICENSE_005("LICENSE_005", HttpStatus.BAD_REQUEST, "授权已过期"),
    LICENSE_006("LICENSE_006", HttpStatus.BAD_REQUEST, "授权实例配额已用尽"),

    // ---- 在线实例 (INSTANCE) ----
    INSTANCE_001("INSTANCE_001", HttpStatus.NOT_FOUND, "实例不存在"),
    INSTANCE_002("INSTANCE_002", HttpStatus.BAD_REQUEST, "实例已离线"),
    INSTANCE_003("INSTANCE_003", HttpStatus.BAD_REQUEST, "实例心跳超时"),
    INSTANCE_004("INSTANCE_004", HttpStatus.BAD_REQUEST, "实例注册信息非法"),

    // ---- 参数校验 (PARAM) ----
    PARAM_001("PARAM_001", HttpStatus.BAD_REQUEST, "请求参数非法"),

    // ---- 系统内部 (SYS) ----
    SYS_001("SYS_001", HttpStatus.INTERNAL_SERVER_ERROR, "系统内部错误");

    private final String code;
    private final HttpStatus httpStatus;
    private final String message;

    ErrorCode(String code, HttpStatus httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 按错误码字符串查找枚举，未命中时返回 {@code null}。
     */
    public static ErrorCode fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ErrorCode errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return null;
    }
}