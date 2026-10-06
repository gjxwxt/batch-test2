package com.example.app.exception;

/**
 * 统一错误码枚举（共享契约）。
 */
public enum ErrorCode {
    SUCCESS("成功"),
    WARNING("警告"),
    AUTH_001("认证失败"),
    AUTH_002("令牌无效或过期"),
    USER_001("用户不存在"),
    USER_003("用户已被禁用"),
    USER_004("密码错误"),
    LICENSE_001("授权不存在"),
    LICENSE_002("授权已过期"),
    LICENSE_003("授权已禁用"),
    LICENSE_004("授权实例数超限"),
    LICENSE_005("授权签名校验失败"),
    LICENSE_006("授权文件格式非法"),
    INSTANCE_001("实例不存在"),
    INSTANCE_002("实例已离线"),
    INSTANCE_003("实例注册失败"),
    INSTANCE_004("实例心跳失败"),
    PARAM_001("参数校验失败"),
    SYS_001("系统内部错误");

    private final String description;

    ErrorCode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}