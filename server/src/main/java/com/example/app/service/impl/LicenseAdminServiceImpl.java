package com.example.app.service.impl;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.LicenseException;
import com.example.app.license.LicensePublicKeyProvider;
import com.example.app.license.LicenseSignature;
import com.example.app.license.LicenseVerifier;
import com.example.app.license.TamperCheckResult;
import com.example.app.model.AuditOperationType;
import com.example.app.model.License;
import com.example.app.model.LicenseImportRequest;
import com.example.app.model.LicenseSummary;
import com.example.app.model.LicenseVerifyResponse;
import com.example.app.repository.InstanceRepository;
import com.example.app.repository.LicenseRepository;
import com.example.app.service.AuditService;
import com.example.app.service.LicenseAdminService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
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
    private final AuditService auditService;
    private final InstanceRepository instanceRepository;

    public LicenseAdminServiceImpl(LicenseRepository licenseRepository,
                                   LicensePublicKeyProvider publicKeyProvider,
                                   LicenseVerifier licenseVerifier,
                                   AuditService auditService,
                                   InstanceRepository instanceRepository) {
        this.licenseRepository = licenseRepository;
        this.publicKeyProvider = publicKeyProvider;
        this.licenseVerifier = licenseVerifier;
        this.auditService = auditService;
        this.instanceRepository = instanceRepository;
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
        License saved = licenseRepository.save(license);
        auditService.record(AuditOperationType.LICENSE_IMPORT, "admin", null,
                saved.serial(), "SUCCESS", "导入授权：" + saved.serial());
        return saved;
    }

    @Override
    public List<LicenseSummary> listLicenses(String status, Integer page, Integer size) {
        // AUTH-023：状态过滤
        List<LicenseSummary> filtered = licenseRepository.findAll().stream()
                .filter(l -> status == null || status.isBlank() || status.equalsIgnoreCase(l.status()))
                .map(LicenseSummary::from)
                .toList();

        // AUTH-023：分页（page 从 1 开始）
        int pageNum = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 20 : size;
        int from = Math.min((pageNum - 1) * pageSize, filtered.size());
        int to = Math.min(from + pageSize, filtered.size());
        List<LicenseSummary> result = filtered.subList(from, to);

        auditService.record(AuditOperationType.LICENSE_QUERY, "admin", null,
                null, "SUCCESS", "查询授权列表，共 " + filtered.size() + " 条（分页 " + pageNum + "/" + pageSize + "）");
        return result;
    }

    @Override
    public long countLicenses(String status) {
        return licenseRepository.findAll().stream()
                .filter(l -> status == null || status.isBlank() || status.equalsIgnoreCase(l.status()))
                .count();
    }

    @Override
    public License getLicense(Long id) {
        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new LicenseException(ErrorCode.LICENSE_001, "授权不存在：" + id));
        auditService.record(AuditOperationType.LICENSE_QUERY, "admin", null,
                license.serial(), "SUCCESS", "查看授权详情：" + license.serial());
        return license;
    }

    @Override
    public LicenseVerifyResponse verifyLicense(Long id) {
        License license = getLicense(id);
        long onlineCount = instanceRepository.countOnlineByLicenseId(license.id());
        TamperCheckResult result = licenseVerifier.analyze(license, onlineCount);

        // AUTH-017：签名破坏 → 授权标记 DISABLED（不可逆），记录审计
        if (!result.signatureValid()) {
            License disabled = markDisabled(license, "授权被篡改：数字签名验证失败");
            auditService.record(AuditOperationType.LICENSE_VERIFY, "admin", null,
                    license.serial(), "FAILED", "授权被篡改，已禁用：" + license.serial());
            return new LicenseVerifyResponse(license.serial(), false,
                    List.of(new LicenseVerifyResponse.LayerResult(
                            "SIGNATURE", false, "数字签名验证失败，授权已标记 DISABLED")),
                    "授权被篡改，已禁用（不可逆）");
        }

        // AUTH-016：签名完好但数据库字段与文件不一致 → 自动修复数据库字段，保持 ACTIVE
        List<String> fixedFields = new ArrayList<>();
        if (!result.fieldMismatches().isEmpty()) {
            license = repairFields(license, result.parsedFields(), result.fieldMismatches(), fixedFields);
        }

        // AUTH-018：配额守恒被破坏（used+remaining≠max）→ 自动修复 remaining=max-used
        boolean quotaFixed = false;
        if (result.quotaConservationBroken()) {
            license = repairQuotaConservation(license);
            quotaFixed = true;
        }

        // 若发生修复则落库并记录审计
        if (!fixedFields.isEmpty() || quotaFixed) {
            License saved = licenseRepository.save(license);
            auditService.record(AuditOperationType.LICENSE_VERIFY, "admin", null,
                    saved.serial(), "SUCCESS",
                    "防篡改自动修复：" + describeFixes(fixedFields, quotaFixed));
            license = saved;
        }

        // AUTH-019：实际在线实例数 > used_instances → 返回 WARNING（passed=true）
        boolean instanceMismatch = onlineCount > (license.usedInstances() != null ? license.usedInstances() : 0);
        if (instanceMismatch) {
            auditService.record(AuditOperationType.LICENSE_VERIFY, "admin", null,
                    license.serial(), "WARNING",
                    "实例数交叉验证不一致：在线 " + onlineCount + " > used " + license.usedInstances());
            return new LicenseVerifyResponse(license.serial(), true,
                    List.of(new LicenseVerifyResponse.LayerResult(
                            "INSTANCE_COUNT", true,
                            "实例数交叉验证不一致：在线 " + onlineCount + " > used " + license.usedInstances())),
                    "WARNING: Instance count mismatch");
        }

        auditService.record(AuditOperationType.LICENSE_VERIFY, "admin", null,
                license.serial(), "SUCCESS", "三层防篡改校验通过");
        return new LicenseVerifyResponse(license.serial(), true,
                List.of(new LicenseVerifyResponse.LayerResult(
                        "SIGNATURE", true, "数字签名验证通过"),
                        new LicenseVerifyResponse.LayerResult(
                                "DB_STATE", true, "授权状态正常（ACTIVE，未过期）"),
                        new LicenseVerifyResponse.LayerResult(
                                "FILE_INTEGRITY", true, "数据库记录与授权文件字段一致")),
                "授权校验通过：签名有效、状态正常、文件一致");
    }

    /** AUTH-017：将授权标记为 DISABLED（不可逆）。 */
    private License markDisabled(License license, String reason) {
        if (license.isDisabled()) {
            return license;
        }
        License updated = new License(
                license.id(), license.serial(), license.licenseName(), license.proname(),
                license.component(), license.version(), license.licensee(), license.licenseMode(),
                license.formal(), license.expiration(), license.userinfor(), license.maxInstances(),
                license.maxCpus(), license.maxMemory(), license.usedInstances(),
                license.remainingInstances(), license.usedCpus(), license.usedMemory(),
                license.bxbFile(), License.STATUS_DISABLED, license.source(),
                license.createTime(), Instant.now());
        return licenseRepository.save(updated);
    }

    /** AUTH-016：将数据库字段恢复为授权文件原始值。 */
    private License repairFields(License license, Map<String, String> fields,
                                 List<String> mismatches, List<String> fixedFields) {
        String serial = license.serial();
        String proname = license.proname();
        String component = license.component();
        String version = license.version();
        String licensee = license.licensee();
        String mode = license.licenseMode();
        String formal = license.formal();
        String expiration = license.expiration();
        String userinfor = license.userinfor();
        Integer maxInstances = license.maxInstances();
        Integer maxCpus = license.maxCpus();
        Integer maxMemory = license.maxMemory();

        for (String field : mismatches) {
            String fileValue = fields.get(field);
            switch (field) {
                case "serial" -> { serial = fileValue; fixedFields.add("serial"); }
                case "proname" -> { proname = fileValue; fixedFields.add("proname"); }
                case "component" -> { component = fileValue; fixedFields.add("component"); }
                case "version" -> { version = fileValue; fixedFields.add("version"); }
                case "licensee" -> { licensee = fileValue; fixedFields.add("licensee"); }
                case "mode" -> { mode = fileValue; fixedFields.add("mode"); }
                case "formal" -> { formal = fileValue; fixedFields.add("formal"); }
                case "expiration" -> { expiration = fileValue; fixedFields.add("expiration"); }
                case "userinfor" -> { userinfor = fileValue; fixedFields.add("userinfor"); }
                case "max-instances" -> { maxInstances = parseInt(fileValue); fixedFields.add("max-instances"); }
                case "max-cpus" -> { maxCpus = parseInt(fileValue); fixedFields.add("max-cpus"); }
                case "max-memory" -> { maxMemory = parseInt(fileValue); fixedFields.add("max-memory"); }
                default -> { }
            }
        }
        return new License(
                license.id(), serial, license.licenseName(), proname, component, version,
                licensee, mode, formal, expiration, userinfor, maxInstances, maxCpus, maxMemory,
                license.usedInstances(), license.remainingInstances(), license.usedCpus(),
                license.usedMemory(), license.bxbFile(), license.status(), license.source(),
                license.createTime(), Instant.now());
    }

    /** AUTH-018：修复配额守恒，remaining = max - used。 */
    private License repairQuotaConservation(License license) {
        int max = license.maxInstances() != null ? license.maxInstances() : 0;
        int used = license.usedInstances() != null ? license.usedInstances() : 0;
        int repairedRemaining = Math.max(0, max - used);
        return new License(
                license.id(), license.serial(), license.licenseName(), license.proname(),
                license.component(), license.version(), license.licensee(), license.licenseMode(),
                license.formal(), license.expiration(), license.userinfor(), license.maxInstances(),
                license.maxCpus(), license.maxMemory(), license.usedInstances(), repairedRemaining,
                license.usedCpus(), license.usedMemory(), license.bxbFile(), license.status(),
                license.source(), license.createTime(), Instant.now());
    }

    private String describeFixes(List<String> fixedFields, boolean quotaFixed) {
        List<String> parts = new ArrayList<>();
        if (!fixedFields.isEmpty()) {
            parts.add("字段修复：" + String.join(",", fixedFields));
        }
        if (quotaFixed) {
            parts.add("配额守恒修复：remaining=max-used");
        }
        return String.join("；", parts);
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
        auditService.record(AuditOperationType.LICENSE_DELETE, "admin", null,
                license.serial(), "SUCCESS", "删除授权：" + license.serial());
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
        License saved = licenseRepository.save(updated);
        auditService.record(AuditOperationType.LICENSE_DISABLE, "admin", null,
                license.serial(), "SUCCESS", "禁用授权：" + license.serial());
        return saved;
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