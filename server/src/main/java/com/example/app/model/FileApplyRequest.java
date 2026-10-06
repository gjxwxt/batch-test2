package com.example.app.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 授权文件申请请求（req-37 / IAS_AUTH_FILE_APPLY）。
 *
 * <p>local/site 模式客户端向授权中心申请授权文件（license.infor），统一掌握各模式授权使用情况。
 * 与前端申请页（AUTH-071）及 API 验收（AUTH-042/043）契约一致：携带 mode 与产品/配额信息。</p>
 *
 * @param mode         授权模式（local / site，必填）
 * @param product      产品标识
 * @param edition      产品版本/版本
 * @param licensee     被授权方名称
 * @param maxInstances 实例配额
 * @param expireAt     到期日期（可选）
 * @param remark       备注（可选）
 */
public record FileApplyRequest(
        @NotBlank(message = "mode must not be blank")
        String mode,

        String product,
        String edition,
        String licensee,

        @NotNull(message = "maxInstances must not be null")
        @Positive(message = "maxInstances must be positive")
        Integer maxInstances,

        String expireAt,
        String remark
) {}