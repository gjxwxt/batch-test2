package com.example.app.controller;

import com.example.app.model.License;
import com.example.app.model.LicenseImportRequest;
import com.example.app.model.LicenseSummary;
import com.example.app.model.LicenseVerifyResponse;
import com.example.app.service.LicenseAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 共享契约 · API 骨架（infra:api-skeleton）— 授权管理端点（wp-3）。
 *
 * <p>路径冻结（6.2 映射）：/api/v1/admin/licenses/**，JWT 鉴权。
 * 控制器仅负责参数绑定、校验、调用 Service 并返回 DTO 与正确 HTTP 状态码。</p>
 */
@RestController
@RequestMapping("/api/v1/admin/licenses")
public class LicenseAdminController {

    private final LicenseAdminService licenseAdminService;

    public LicenseAdminController(LicenseAdminService licenseAdminService) {
        this.licenseAdminService = licenseAdminService;
    }

    /**
     * IAS_AUTH_IMPORT — 授权导入。
     */
    @PostMapping("/import")
    public ResponseEntity<License> importLicense(@Valid @RequestBody LicenseImportRequest request) {
        License created = licenseAdminService.importLicense(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * IAS_AUTH_LIST — 授权列表查询（AUTH-023 支持状态过滤与分页）。
     *
     * <p>分页元数据（total）通过响应头 {@code X-Total-Count} 返回，保持响应体为授权数组
     * （与前端 {@code LicenseSummary[]} 契约兼容）。</p>
     */
    @GetMapping
    public ResponseEntity<List<LicenseSummary>> listLicenses(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        long total = licenseAdminService.countLicenses(status);
        List<LicenseSummary> result = licenseAdminService.listLicenses(status, page, size);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(total))
                .body(result);
    }

    /**
     * IAS_AUTH_DETAIL — 授权详情查看。
     */
    @GetMapping("/{id}")
    public ResponseEntity<License> getLicense(@PathVariable Long id) {
        return ResponseEntity.ok(licenseAdminService.getLicense(id));
    }

    /**
     * IAS_AUTH_TAMPER_CHECK — 三层防篡改校验。
     */
    @GetMapping("/{id}/verify")
    public ResponseEntity<LicenseVerifyResponse> verifyLicense(@PathVariable Long id) {
        return ResponseEntity.ok(licenseAdminService.verifyLicense(id));
    }

    /**
     * IAS_AUTH_DELETE — 授权删除。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLicense(@PathVariable Long id) {
        licenseAdminService.deleteLicense(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * IAS_AUTH_DISABLE — 授权禁用。
     */
    @PutMapping("/{id}/disable")
    public ResponseEntity<License> disableLicense(@PathVariable Long id) {
        return ResponseEntity.ok(licenseAdminService.disableLicense(id));
    }
}