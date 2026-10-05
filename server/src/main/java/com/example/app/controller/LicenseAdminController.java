package com.example.app.controller;

import com.example.app.exception.ApiErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 共享契约 · API 骨架（infra:api-skeleton）— 授权管理端点。
 *
 * <p>路径冻结（6.2 映射）：/api/v1/admin/licenses/**。业务逻辑由并行工作包 wp-3（授权管理）实现。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/licenses")
public class LicenseAdminController {

    @PostMapping("/import")
    public ResponseEntity<ApiErrorResponse> importLicense() {
        // TODO(wp-3): IAS_AUTH_IMPORT — 授权导入
        throw new UnsupportedOperationException("wp-3: IAS_AUTH_IMPORT not yet implemented");
    }

    @GetMapping
    public ResponseEntity<ApiErrorResponse> listLicenses() {
        // TODO(wp-3): IAS_AUTH_LIST — 授权列表查询
        throw new UnsupportedOperationException("wp-3: IAS_AUTH_LIST not yet implemented");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiErrorResponse> getLicense(@PathVariable Long id) {
        // TODO(wp-3): IAS_AUTH_DETAIL — 授权详情查看
        throw new UnsupportedOperationException("wp-3: IAS_AUTH_DETAIL not yet implemented");
    }

    @GetMapping("/{id}/verify")
    public ResponseEntity<ApiErrorResponse> verifyLicense(@PathVariable Long id) {
        // TODO(wp-3): IAS_AUTH_TAMPER_CHECK — 三层防篡改校验
        throw new UnsupportedOperationException("wp-3: IAS_AUTH_TAMPER_CHECK not yet implemented");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiErrorResponse> deleteLicense(@PathVariable Long id) {
        // TODO(wp-3): IAS_AUTH_DELETE — 授权删除
        throw new UnsupportedOperationException("wp-3: IAS_AUTH_DELETE not yet implemented");
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<ApiErrorResponse> disableLicense(@PathVariable Long id) {
        // TODO(wp-3): IAS_AUTH_DISABLE — 授权禁用
        throw new UnsupportedOperationException("wp-3: IAS_AUTH_DISABLE not yet implemented");
    }
}