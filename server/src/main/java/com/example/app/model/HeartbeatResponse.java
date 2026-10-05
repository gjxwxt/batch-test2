package com.example.app.model;

/**
 * 客户端心跳响应（req-16 / IAS_AUTH_HEARTBEAT）。
 *
 * @param instanceId 实例标识
 * @param status     实例状态（ONLINE）
 * @param serverTime 服务端时间（ISO 8601）
 * @param message    提示信息
 */
public record HeartbeatResponse(
        String instanceId,
        String status,
        String serverTime,
        String message
) {}