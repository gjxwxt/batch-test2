package com.example.app.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 客户端心跳请求（req-16 / IAS_AUTH_HEARTBEAT）。
 *
 * <p>客户端周期性上报实例存活状态，请求体含通信签名（infra:signature）。</p>
 *
 * @param instanceId 实例标识（必填）
 * @param serial     授权序列号
 * @param currentCpus 当前 CPU 数
 * @param currentMemory 当前内存 MB
 * @param signature  通信签名（Base64）
 */
public record HeartbeatRequest(
        @NotBlank(message = "instanceId must not be blank")
        String instanceId,

        String serial,
        Integer currentCpus,
        Integer currentMemory,
        String signature
) {}