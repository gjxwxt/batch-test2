package com.example.app.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 授权文件申请请求（req-37 / IAS_AUTH_FILE_APPLY）。
 *
 * <p>local/site 模式客户端向授权中心申请授权文件（license.infor），统一掌握各模式授权使用情况。</p>
 *
 * @param serial      授权序列号（必填）
 * @param clientUuid  客户端 UUID
 * @param proname     产品标识
 * @param productType 产品类型
 * @param productVersion 产品版本
 * @param hostname    主机名
 * @param ipAddress   IP 地址
 * @param mac         MAC 地址
 * @param signature   通信签名（Base64）
 */
public record FileApplyRequest(
        @NotBlank(message = "serial must not be blank")
        String serial,

        String clientUuid,
        String proname,
        String productType,
        String productVersion,
        String hostname,
        String ipAddress,
        String mac,
        String signature
) {}