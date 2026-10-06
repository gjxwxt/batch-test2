package com.example.app.license;

import com.example.app.model.License;
import com.example.app.model.LicenseVerifyResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 三层防篡改校验（IAS_AUTH_TAMPER_CHECK）。
 *
 * <p>对授权执行三层校验，任一层失败即整体判定为篡改/失效：</p>
 * <ol>
 *   <li><b>SIGNATURE</b> — 数字签名验证：RSA-2048 + SHA256withRSA 对 canonical 字段验签，
 *       证明授权文件由可信授权中心签发且未被修改（infra:signature）。</li>
 *   <li><b>DB_STATE</b> — 数据库状态校验：授权记录存在、状态为 ACTIVE（未禁用）、未过期。</li>
 *   <li><b>FILE_INTEGRITY</b> — 存储文件一致性校验：数据库记录字段与存储的授权文件（bxb_file）
 *       解析字段一致，防止库表与文件被分别篡改。</li>
 * </ol>
 */
@Component
public class LicenseVerifier {

    private final LicensePublicKeyProvider publicKeyProvider;

    public LicenseVerifier(LicensePublicKeyProvider publicKeyProvider) {
        this.publicKeyProvider = publicKeyProvider;
    }

    /**
     * 对授权记录执行三层防篡改校验。
     *
     * @param license 数据库中的授权记录（含 bxb_file 原始 XML）
     * @return 校验结果（含各层明细）
     */
    public LicenseVerifyResponse verify(License license) {
        List<LicenseVerifyResponse.LayerResult> layers = new ArrayList<>();

        // Layer 1: 数字签名验证
        layers.add(verifySignatureLayer(license));

        // Layer 2: 数据库状态校验
        layers.add(verifyDbStateLayer(license));

        // Layer 3: 存储文件一致性校验
        layers.add(verifyFileIntegrityLayer(license));

        boolean verified = layers.stream().allMatch(LicenseVerifyResponse.LayerResult::passed);
        String message = verified
                ? "授权校验通过：签名有效、状态正常、文件一致"
                : "授权校验失败：存在篡改或状态异常";

        return new LicenseVerifyResponse(license.serial(), verified, layers, message);
    }

    /**
     * 增强三层防篡改分析（AUTH-016/017/018/019）。
     *
     * <p>在基础三层校验之上，额外返回供服务层执行自动修复/禁用/警告所需的结构化信息。
     * 与 {@link #verify(License)} 不同，本方法不直接判定整体通过与否，而是暴露
     * 签名有效性、字段不一致清单、配额守恒状态与实际在线实例数，由服务层据此落库修复。</p>
     *
     * @param license            数据库中的授权记录（含 bxb_file 原始 XML）
     * @param onlineInstanceCount 该授权下实际在线实例数（AUTH-019 交叉验证）
     * @return 增强分析结果
     */
    public TamperCheckResult analyze(License license, long onlineInstanceCount) {
        Map<String, String> fields;
        boolean signatureValid;
        try {
            fields = LicenseSignature.parseLicenseXml(license.bxbFile());
            LicenseSignature.verifySignature(fields, publicKeyProvider.getPublicKey());
            signatureValid = true;
        } catch (Exception e) {
            fields = Map.of();
            signatureValid = false;
        }

        boolean dbStateValid = !license.isDisabled()
                && !license.isExpired()
                && !license.isPastExpiration();

        List<String> mismatches = new ArrayList<>();
        if (signatureValid) {
            collectFieldMismatches(license, fields, mismatches);
        }

        boolean quotaConservationBroken = isQuotaConservationBroken(license);

        return new TamperCheckResult(
                signatureValid,
                dbStateValid,
                fields,
                mismatches,
                quotaConservationBroken,
                onlineInstanceCount
        );
    }

    private void collectFieldMismatches(License license, Map<String, String> fields,
                                        List<String> mismatches) {
        checkField(mismatches, "serial", fields.get("serial"), license.serial());
        checkField(mismatches, "proname", fields.get("proname"), license.proname());
        checkField(mismatches, "component", fields.get("component"), license.component());
        checkField(mismatches, "version", fields.get("version"), license.version());
        checkField(mismatches, "licensee", fields.get("licensee"), license.licensee());
        checkField(mismatches, "mode", fields.get("mode"), license.licenseMode());
        checkField(mismatches, "formal", fields.get("formal"), license.formal());
        checkField(mismatches, "expiration", fields.get("expiration"), license.expiration());
        checkField(mismatches, "userinfor", fields.get("userinfor"), license.userinfor());
        checkField(mismatches, "max-instances",
                fields.get("max-instances"), stringOf(license.maxInstances()));
        checkField(mismatches, "max-cpus", fields.get("max-cpus"), stringOf(license.maxCpus()));
        checkField(mismatches, "max-memory", fields.get("max-memory"), stringOf(license.maxMemory()));
    }

    /**
     * 比较单个字段，不一致时仅记录字段名（供服务层 AUTH-016 自动修复定位）。
     */
    private void checkField(List<String> mismatches, String field, String fileValue, String dbValue) {
        String normalizedFile = fileValue == null ? "" : fileValue.trim();
        String normalizedDb = dbValue == null ? "" : dbValue.trim();
        if (!normalizedFile.equals(normalizedDb)) {
            mismatches.add(field);
        }
    }

    private boolean isQuotaConservationBroken(License license) {
        Integer max = license.maxInstances();
        Integer used = license.usedInstances();
        Integer remaining = license.remainingInstances();
        if (max == null || used == null || remaining == null) {
            return false;
        }
        return used + remaining != max;
    }

    private LicenseVerifyResponse.LayerResult verifySignatureLayer(License license) {
        try {
            Map<String, String> fields = LicenseSignature.parseLicenseXml(license.bxbFile());
            LicenseSignature.verifySignature(fields, publicKeyProvider.getPublicKey());
            return new LicenseVerifyResponse.LayerResult(
                    "SIGNATURE", true, "数字签名验证通过（RSA-2048 + SHA256withRSA）");
        } catch (Exception e) {
            return new LicenseVerifyResponse.LayerResult(
                    "SIGNATURE", false, "数字签名验证失败：" + e.getMessage());
        }
    }

    private LicenseVerifyResponse.LayerResult verifyDbStateLayer(License license) {
        if (license.isDisabled()) {
            return new LicenseVerifyResponse.LayerResult(
                    "DB_STATE", false, "授权已被禁用（status=DISABLED）");
        }
        if (license.isExpired()) {
            return new LicenseVerifyResponse.LayerResult(
                    "DB_STATE", false, "授权状态为已过期（status=EXPIRED）");
        }
        if (license.isPastExpiration()) {
            return new LicenseVerifyResponse.LayerResult(
                    "DB_STATE", false, "授权已超过有效期：" + license.expiration());
        }
        return new LicenseVerifyResponse.LayerResult(
                "DB_STATE", true, "授权状态正常（ACTIVE，未过期）");
    }

    private LicenseVerifyResponse.LayerResult verifyFileIntegrityLayer(License license) {
        try {
            Map<String, String> fields = LicenseSignature.parseLicenseXml(license.bxbFile());
            List<String> mismatches = new ArrayList<>();

            checkField(mismatches, "serial", fields.get("serial"), license.serial());
            checkField(mismatches, "proname", fields.get("proname"), license.proname());
            checkField(mismatches, "component", fields.get("component"), license.component());
            checkField(mismatches, "version", fields.get("version"), license.version());
            checkField(mismatches, "licensee", fields.get("licensee"), license.licensee());
            checkField(mismatches, "mode", fields.get("mode"), license.licenseMode());
            checkField(mismatches, "formal", fields.get("formal"), license.formal());
            checkField(mismatches, "expiration", fields.get("expiration"), license.expiration());
            checkField(mismatches, "userinfor", fields.get("userinfor"), license.userinfor());
            checkField(mismatches, "max-instances",
                    fields.get("max-instances"), stringOf(license.maxInstances()));
            checkField(mismatches, "max-cpus", fields.get("max-cpus"), stringOf(license.maxCpus()));
            checkField(mismatches, "max-memory", fields.get("max-memory"), stringOf(license.maxMemory()));

            if (mismatches.isEmpty()) {
                return new LicenseVerifyResponse.LayerResult(
                        "FILE_INTEGRITY", true, "数据库记录与授权文件字段一致");
            }
            return new LicenseVerifyResponse.LayerResult(
                    "FILE_INTEGRITY", false, "字段不一致：" + String.join("; ", mismatches));
        } catch (Exception e) {
            return new LicenseVerifyResponse.LayerResult(
                    "FILE_INTEGRITY", false, "授权文件解析失败：" + e.getMessage());
        }
    }

    private String stringOf(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }
}