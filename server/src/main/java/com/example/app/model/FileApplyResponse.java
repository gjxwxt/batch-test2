package com.example.app.model;

/**
 * 授权文件申请响应（req-37 / IAS_AUTH_FILE_APPLY）。
 *
 * @param serial      授权序列号
 * @param licenseFile 授权文件原始 XML 文本（license.infor）
 * @param licenseMode 授权模式（local / site）
 * @param message     提示信息
 */
public record FileApplyResponse(
        String serial,
        String licenseFile,
        String licenseMode,
        String message
) {}