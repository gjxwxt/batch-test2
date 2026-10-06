package com.example.app.service.impl;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.LicenseException;
import com.example.app.license.LicensePublicKeyProvider;
import com.example.app.license.LicenseSignature;
import com.example.app.license.LicenseVerifier;
import com.example.app.model.License;
import com.example.app.model.LicenseImportRequest;
import com.example.app.model.LicenseSummary;
import com.example.app.model.LicenseVerifyResponse;
import com.example.app.repository.LicenseRepository;
import com.example.app.service.LicenseAdminService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 授权管理服务实现（wp-3）。
 */
@Service
public class LicenseAdminServiceImpl implements LicenseAdminService {

    private final LicenseRepository licenseRepository;
    private final LicensePublicKeyProvider publicKeyProvider;
    private final LicenseVerifier licenseVerifier;

    public LicenseAdminServiceImpl(LicenseRepository licenseRepository,
                                   LicensePublicKeyProvider publicKeyProvider,
                                   LicenseVerifier licenseVerifier) {
        this.licenseRepository = licenseRepository;
        this.publicKeyProvider = publicKeyProvider;
        this.licenseVerifier = licenseVerifier;
    }

    @Override
    public License importLicense(LicenseImportRequest request) {
        // 1. 解析授权文件
        Map<String, String> fields = LicenseSignature.parseLicenseXml(request.licenseFile());

        // 2. 数字签名验证（LICENSE_002）
        LicenseSignature.verifySignature(fields, publicKeyProvider.getPublicKey());

        // 3. 查重（LICENSE_005）
        String serial = fields.getOrDefault("serial", "");
        if (serial.isBlank()) {
            throw new LicenseException(ErrorCode.PARAM_001, "授权文件缺少序列号");
        }
        if (licenseRepository.existsBySerial(serial)) {
            throw new LicenseException(ErrorCode.LICENSE_005, "授权已存在（重复导入）：" + serial);
        }

        // 4. 有效期校验（LICENSE_004）
        String expiration = fields.getOrDefault("expiration", "");
        if (isExpired(expiration)) {
            throw new LicenseException(ErrorCode.LICENSE_004, "授权已过期：" + expiration);
        }

        // 5. 落库
        Instant now = Instant.now();
        License license = new License(
                null,
                serial,
                request.licenseName(),
                fields.getOrDefault("proname", ""),
                fields.getOrDefault("component", ""),
                fields.getOrDefault("version", ""),
                fields.getOrDefault("licensee", ""),
                fields.getOrDefault("mode", ""),
                fields.getOrDefault("formal", ""),
                expiration,
                fields.getOrDefault("userinfor", ""),
                parseInt(fields.get("max-instances")),
                parseInt(fields.get("max-cpus")),
                parseInt(fields.get("max-memory")),
                0,
                parseInt(fields.get("max-instances")),
                0,
                0,
                request.licenseFile(),
                License.STATUS_ACTIVE,
                "FILE",
                now,
                now
        );
        return licenseRepository.save(license);
    }

    @Override
    public List<LicenseSummary> listLicenses() {
        return licenseRepository.findAll().stream()
                .map(LicenseSummary::from)
                .toList();
    }

    @Override
    public License getLicense(Long id) {
        return licenseRepository.findById(id)
                .orElseThrow(() -> new LicenseException(ErrorCode.LICENSE_001, "授权不存在：" + id));
    }

    @Override
    public LicenseVerifyResponse verifyLicense(Long id) {
        License license = getLicense(id);
        return licenseVerifier.verify(license);
    }

    @Override
    public void deleteLicense(Long id) {
        License license = getLicense(id);
        // 存在在线实例时拒绝删除（LICENSE_006）
        if (license.usedInstances() != null && license.usedInstances() > 0) {
            throw new LicenseException(ErrorCode.LICENSE_006,
                    "存在在线实例，无法删除授权：" + license.serial());
        }
        licenseRepository.deleteById(id);
    }

    @Override
    public License disableLicense(Long id) {
        License license = getLicense(id);
        if (license.isDisabled()) {
            return license;
        }
        License updated = new License(
                license.id(),
                license.serial(),
                license.licenseName(),
                license.proname(),
                license.component(),
                license.version(),
                license.licensee(),
                license.licenseMode(),
                license.formal(),
                license.expiration(),
                license.userinfor(),
                license.maxInstances(),
                license.maxCpus(),
                license.maxMemory(),
                license.usedInstances(),
                license.remainingInstances(),
                license.usedCpus(),
                license.usedMemory(),
                license.bxbFile(),
                License.STATUS_DISABLED,
                license.source(),
                license.createTime(),
                Instant.now()
        );
        return licenseRepository.save(updated);
    }

    private boolean isExpired(String expiration) {
        if (expiration == null || expiration.isBlank() || "never".equalsIgnoreCase(expiration)) {
            return false;
        }
        try {
            java.time.LocalDate expiryDate = java.time.LocalDate.parse(expiration);
            return expiryDate.isBefore(java.time.LocalDate.now());
        } catch (Exception e) {
            throw new LicenseException(ErrorCode.PARAM_001, "授权有效期格式非法：" + expiration);
        }
    }

    private Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}