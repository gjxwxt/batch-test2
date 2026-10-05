package com.example.app.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 实例注册请求（req-14 / IAS_AUTH_REGISTER）。
 *
 * <p>客户端携带授权序列号与实例信息注册，请求体含通信签名（infra:signature）。</p>
 *
 * @param serial      授权序列号（必填）
 * @param clientUuid  客户端 UUID
 * @param proname     产品标识
 * @param productType 产品类型
 * @param productVersion 产品版本
 * @param productSpec 产品规格
 * @param hostname    主机名
 * @param ipAddress   IP 地址
 * @param mac         MAC 地址
 * @param machineType 机器类型
 * @param currentCpus 当前 CPU 数
 * @param currentMemory 当前内存 MB
 * @param extendedAttributes 扩展属性 JSON
 * @param signature   通信签名（Base64）
 */
public record RegisterRequest(
        @NotBlank(message = "serial must not be blank")
        String serial,

        String clientUuid,
        String proname,
        String productType,
        String productVersion,
        String productSpec,
        String hostname,
        String ipAddress,
        String mac,
        String machineType,
        Integer currentCpus,
        Integer currentMemory,
        String extendedAttributes,
        String signature
) {}