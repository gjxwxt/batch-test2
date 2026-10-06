package com.example.app.model;

/**
 * 授权文件申请响应（req-37 / IAS_AUTH_FILE_APPLY）。
 *
 * <p>与前端申请页（AUTH-071）契约一致：提交后返回申请记录，状态为「待签发」。</p>
 *
 * @param applyId 申请单号
 * @param mode    授权模式（local / site）
 * @param status  申请状态（PENDING 待签发）
 * @param message 提示信息
 */
public record FileApplyResponse(
        String applyId,
        String mode,
        String status,
        String message
) {}