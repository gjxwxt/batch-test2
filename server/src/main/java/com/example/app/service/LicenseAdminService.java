package com.example.app.service;

import com.example.app.model.License;
import com.example.app.model.LicenseImportRequest;
import com.example.app.model.LicenseSummary;
import com.example.app.model.LicenseVerifyResponse;

import java.util.List;

/**
 * 授权管理服务（wp-3 授权管理）。
 *
 * <p>承载授权导入 / 列表 / 详情 / 防篡改校验 / 删除 / 禁用等业务规则。</p>
 */
public interface LicenseAdminService {

    /**
     * 导入授权（IAS_AUTH_IMPORT）。
     * 解析授权文件、验签、查重、落库。
     *
     * @return 导入后的授权记录
     */
    License importLicense(LicenseImportRequest request);

    /**
     * 授权列表（IAS_AUTH_LIST）。
     *
     * @param status 状态过滤（ACTIVE / DISABLED / EXPIRED，可空表示不过滤）
     * @param page   页码（从 1 开始，可空默认 1）
     * @param size   每页条数（可空默认 20）
     * @return 过滤并分页后的授权概要列表
     */
    List<LicenseSummary> listLicenses(String status, Integer page, Integer size);

    /**
     * 统计符合状态过滤条件的授权总数（AUTH-023 分页 total）。
     */
    long countLicenses(String status);

    /**
     * 授权详情（IAS_AUTH_DETAIL）。
     */
    License getLicense(Long id);

    /**
     * 三层防篡改校验（IAS_AUTH_TAMPER_CHECK）。
     */
    LicenseVerifyResponse verifyLicense(Long id);

    /**
     * 删除授权（IAS_AUTH_DELETE）。
     * 存在在线实例时拒绝删除（LICENSE_006）。
     */
    void deleteLicense(Long id);

    /**
     * 禁用授权（IAS_AUTH_DISABLE）。
     */
    License disableLicense(Long id);
}