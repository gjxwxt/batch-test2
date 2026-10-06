package com.example.app.model;

/**
 * 实例注册响应（req-14 / IAS_AUTH_REGISTER）。
 *
 * @param instanceId  已注册实例标识
 * @param status      实例状态（ONLINE）
 * @param heartbeatInterval 心跳间隔（秒）
 * @param message     提示信息（如弹性运行模式警告）
 */
public record RegisterResponse(
        String instanceId,
        String status,
        Integer heartbeatInterval,
        String message
) {}