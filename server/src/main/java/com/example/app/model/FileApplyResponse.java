package com.example.app.model;

/**
 * 授权文件申请响应（req-37 / IAS_AUTH_FILE_APPLY）。
 *
 * <p>local/site 模式客户端申请授权文件，服务端生成并签名后返回授权文件内容
 * （AUTH-042/043）。</p>
 *
 * @param applyId   申请单号
 * @param mode      授权模式（local / site）
 * @param status    申请状态（SUCCESS）
 * @param message   提示信息
 * @param licenseFile 生成的授权文件内容（license.infor XML，含签名）
 */
public record FileApplyResponse(
        String applyId,
        String mode,
        String status,
        String message,
        String licenseFile
) {}