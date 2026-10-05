package com.example.app.exception;

/**
 * 统一错误码枚举（冻结契约 infra:error-codes）。
 * 所有 controller/service 不得自行发明错误码，一律引用本枚举。
 */
public enum ErrorCode {

    SUCCESS("SUCCESS", "操作成功"),
    WARNING("WARNING", "操作成功（有警告）"),
    AUTH_001("AUTH_001", "用户名或密码错误"),
    AUTH_002("AUTH_002", "登录已过期"),
    USER_001("USER_001", "用户不存在"),
    USER_003("USER_003", "旧密码错误"),
    USER_004("USER_004", "新密码复杂度不足"),
    LICENSE_001("LICENSE_001", "授权不存在"),
    LICENSE_002("LICENSE_002", "授权签名验证失败"),
    LICENSE_003("LICENSE_003", "CPU/内存配额不足"),
    LICENSE_004("LICENSE_004", "授权已过期"),
    LICENSE_005("LICENSE_005", "授权已存在（重复导入）"),
    LICENSE_006("LICENSE_006", "存在在线实例，无法删除"),
    INSTANCE_001("INSTANCE_001", "弹性运行模式提示"),
    INSTANCE_002("INSTANCE_002", "实例不存在"),
    INSTANCE_003("INSTANCE_003", "授权已过期"),
    INSTANCE_004("INSTANCE_004", "配额超 200% 上限"),
    PARAM_001("PARAM_001", "参数缺失或格式错误"),
    SYS_001("SYS_001", "服务器内部错误");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}