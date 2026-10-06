package com.example.app.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 心跳配置更新请求。
 */
public record HeartbeatConfigRequest(
        @NotNull(message = "heartbeatInterval must not be null")
        @Min(value = 1, message = "heartbeatInterval must be at least 1 second")
        @Max(value = 86400, message = "heartbeatInterval must not exceed 86400 seconds")
        Integer heartbeatInterval,

        @NotNull(message = "timeoutMultiplier must not be null")
        @Min(value = 1, message = "timeoutMultiplier must be at least 1")
        @Max(value = 10, message = "timeoutMultiplier must not exceed 10")
        Integer timeoutMultiplier
) {}