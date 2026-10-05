package com.example.app.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 授权导入请求（IAS_AUTH_IMPORT）。
 *
 * <p>客户端上传授权文件原始 XML 文本，服务端解析、验签并落库。</p>
 *
 * @param licenseFile 授权文件原始 XML 文本（必填）
 * @param licenseName 自定义授权名称（可选，便于管理台识别）
 */
public record LicenseImportRequest(
        @NotBlank(message = "licenseFile must not be blank")
        String licenseFile,

        String licenseName
) {}