package com.example.app.model;

/**
 * 心跳参数获取响应（req-15 / IAS_AUTH_HB_CONFIG）。
 *
 * @param heartbeatInterval 心跳间隔（秒）
 * @param timeoutCount      超时倍数（心跳间隔 × 此倍数为超时阈值）
 * @param timeoutSeconds    超时阈值（秒）
 */
public record HeartbeatConfigResponse(
        Integer heartbeatInterval,
        Integer timeoutCount,
        Integer timeoutSeconds
) {}