package com.example.app.model;

import java.util.List;

/**
 * 授权防篡改校验响应（IAS_AUTH_TAMPER_CHECK）。
 *
 * <p>三层防篡改校验结果：数字签名 / 数据库状态 / 存储文件一致性。</p>
 *
 * @param serial      授权序列号
 * @param verified    是否全部通过
 * @param layers      各层校验结果明细
 * @param message     校验结论描述
 */
public record LicenseVerifyResponse(
        String serial,
        boolean verified,
        List<LayerResult> layers,
        String message
) {

    /**
     * 单层校验结果。
     *
     * @param name    层名称（如 SIGNATURE / DB_STATE / FILE_INTEGRITY）
     * @param passed  是否通过
     * @param detail  校验明细
     */
    public record LayerResult(
            String name,
            boolean passed,
            String detail
    ) {}
}