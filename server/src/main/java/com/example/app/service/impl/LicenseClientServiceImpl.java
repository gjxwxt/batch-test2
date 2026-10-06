package com.example.app.service.impl;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.LicenseException;
import com.example.app.license.CommunicationKeyProvider;
import com.example.app.license.CommunicationSignature;
import com.example.app.license.LicenseFileGenerator;
import com.example.app.model.AuditOperationType;
import com.example.app.model.FileApplyRequest;
import com.example.app.model.FileApplyResponse;
import com.example.app.model.HeartbeatConfigResponse;
import com.example.app.model.HeartbeatRequest;
import com.example.app.model.HeartbeatResponse;
import com.example.app.model.Instance;
import com.example.app.model.License;
import com.example.app.model.PublicKeyResponse;
import com.example.app.model.RegisterRequest;
import com.example.app.model.RegisterResponse;
import com.example.app.repository.InstanceRepository;
import com.example.app.repository.LicenseRepository;
import com.example.app.repository.SystemConfigRepository;
import com.example.app.service.AuditService;
import com.example.app.service.LicenseClientService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端交互服务实现（wp-4）。
 */
@Service
public class LicenseClientServiceImpl implements LicenseClientService {

    private final CommunicationKeyProvider communicationKeyProvider;
    private final LicenseRepository licenseRepository;
    private final InstanceRepository instanceRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final AuditService auditService;
    private final LicenseFileGenerator licenseFileGenerator;

    public LicenseClientServiceImpl(CommunicationKeyProvider communicationKeyProvider,
                                    LicenseRepository licenseRepository,
                                    InstanceRepository instanceRepository,
                                    SystemConfigRepository systemConfigRepository,
                                    AuditService auditService,
                                    LicenseFileGenerator licenseFileGenerator) {
        this.communicationKeyProvider = communicationKeyProvider;
        this.licenseRepository = licenseRepository;
        this.instanceRepository = instanceRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.auditService = auditService;
        this.licenseFileGenerator = licenseFileGenerator;
    }

    @Override
    public PublicKeyResponse getPublicKey() {
        return new PublicKeyResponse(
                CommunicationKeyProvider.KEY_ALGORITHM,
                CommunicationKeyProvider.KEY_SIZE,
                communicationKeyProvider.getPublicKeyBase64()
        );
    }

    @Override
    public HeartbeatConfigResponse getHeartbeatConfig() {
        int interval = parseIntConfig(SystemConfigRepository.KEY_HEARTBEAT_INTERVAL, 30);
        int timeoutCount = parseIntConfig(SystemConfigRepository.KEY_HEARTBEAT_TIMEOUT_COUNT, 3);
        return new HeartbeatConfigResponse(interval, timeoutCount, interval * timeoutCount);
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        // 1. 校验通信签名（LICENSE_002）
        verifyRequestSignature(buildRegisterCanonical(request), request.signature());

        // 2. 查找授权（LICENSE_001）
        License license = findActiveLicense(request.serial());

        // 3. proname 必填（AUTH-032 / PARAM_001）
        if (request.proname() == null || request.proname().isBlank()) {
            throw new LicenseException(ErrorCode.PARAM_001, "proname 不能为空");
        }

        // 4. 已存在实例则执行重注册（req-17）
        String instanceId = request.clientUuid() != null && !request.clientUuid().isBlank()
                ? request.clientUuid()
                : "inst-" + UUID.randomUUID().toString().substring(0, 8);

        Instance existing = instanceRepository.findByInstanceId(instanceId).orElse(null);
        if (existing != null) {
            return reRegister(existing, license, request);
        }

        // 5. CPU/内存配额校验（AUTH-030/031 / LICENSE_003）
        validateCpuMemoryQuota(license, request);

        // 6. 原子占用实例配额（req-27 弹性配额 / req-28 多节点一致性 CAS）
        int elasticMultiplier = getElasticQuotaMultiplier();
        boolean acquired = licenseRepository.tryAcquireInstanceQuota(license.id(), elasticMultiplier);
        if (!acquired) {
            throw new LicenseException(ErrorCode.INSTANCE_004,
                    "配额超弹性上限（" + elasticMultiplier + " 倍）拒绝注册");
        }

        // 7. 占用 CPU/内存配额（AUTH-030/031）
        licenseRepository.acquireCpuMemoryQuota(license.id(), request.currentCpus(), request.currentMemory());

        // 8. 创建实例记录
        Instant now = Instant.now();
        Instance instance = new Instance(
                null,
                instanceId,
                license.id(),
                request.clientUuid(),
                request.proname(),
                request.productType(),
                request.productVersion(),
                request.productSpec(),
                request.hostname(),
                request.ipAddress(),
                request.mac(),
                request.machineType(),
                request.currentCpus(),
                request.currentMemory(),
                request.extendedAttributes(),
                Instance.STATUS_ONLINE,
                now,
                now,
                null,
                now,
                now
        );
        instanceRepository.save(instance);

        int interval = parseIntConfig(SystemConfigRepository.KEY_HEARTBEAT_INTERVAL, 30);
        // 弹性区间判定：used > max 返回 WARNING（AUTH-028/055），否则 SUCCESS
        License updated = licenseRepository.findBySerial(license.serial()).orElse(license);
        int used = updated.usedInstances() != null ? updated.usedInstances() : 0;
        int max = updated.maxInstances() != null ? updated.maxInstances() : 0;
        boolean elastic = max > 0 && used > max;
        String code = elastic ? "WARNING" : "SUCCESS";
        String message = elastic
                ? "弹性运行模式：实例数已超过基础配额（" + max + "），当前 " + used
                : "注册成功";
        auditService.record(AuditOperationType.REGISTER, "client", null,
                license.serial(), code, "实例注册：" + instanceId + "（" + message + "）");
        return new RegisterResponse(code, instanceId, Instance.STATUS_ONLINE, interval, message);
    }

    @Override
    public HeartbeatResponse heartbeat(HeartbeatRequest request) {
        // 1. 校验通信签名（LICENSE_002）
        verifyRequestSignature(buildHeartbeatCanonical(request), request.signature());

        // 2. 查找实例（INSTANCE_002）
        Instance instance = instanceRepository.findByInstanceId(request.instanceId())
                .orElseThrow(() -> new LicenseException(ErrorCode.INSTANCE_002,
                        "实例不存在：" + request.instanceId()));

        // 3. 授权有效性校验（AUTH-040）：授权已禁用/过期 → INSTANCE_003，客户端继续心跳但告警
        License license = licenseRepository.findById(instance.licenseId()).orElse(null);
        if (license != null && (license.isDisabled()
                || license.isExpired() || license.isPastExpiration())) {
            auditService.record(AuditOperationType.HEARTBEAT, "client", null,
                    request.instanceId(), "WARNING", "授权失效，心跳告警：" + license.serial());
            throw new LicenseException(ErrorCode.INSTANCE_003,
                    "授权已失效（禁用或过期）：" + license.serial());
        }

        // 5. 下线恢复（AUTH-038）：实例曾因超时被标记 OFFLINE，现恢复心跳 → 重新占用配额
        boolean recovering = !Instance.STATUS_ONLINE.equals(instance.status());
        if (recovering && license != null) {
            int elasticMultiplier = getElasticQuotaMultiplier();
            licenseRepository.tryAcquireInstanceQuota(license.id(), elasticMultiplier);
            licenseRepository.acquireCpuMemoryQuota(license.id(),
                    request.currentCpus() != null ? request.currentCpus() : instance.currentCpus(),
                    request.currentMemory() != null ? request.currentMemory() : instance.currentMemory());
        }

        // 6. 更新心跳时间
        Instant now = Instant.now();
        Instance updated = new Instance(
                instance.id(),
                instance.instanceId(),
                instance.licenseId(),
                instance.clientUuid(),
                instance.proname(),
                instance.productType(),
                instance.productVersion(),
                instance.productSpec(),
                instance.hostname(),
                instance.ipAddress(),
                instance.mac(),
                instance.machineType(),
                request.currentCpus() != null ? request.currentCpus() : instance.currentCpus(),
                request.currentMemory() != null ? request.currentMemory() : instance.currentMemory(),
                instance.extendedAttributes(),
                Instance.STATUS_ONLINE,
                instance.onlineTime(),
                now,
                null,
                instance.createTime(),
                now
        );
        instanceRepository.save(updated);

        auditService.record(AuditOperationType.HEARTBEAT, "client", null,
                request.instanceId(), "SUCCESS",
                recovering ? "客户端心跳（下线恢复，重新占用配额）：" + request.instanceId()
                        : "客户端心跳：" + request.instanceId());
        return new HeartbeatResponse(request.instanceId(), Instance.STATUS_ONLINE, now.toString(), "心跳成功");
    }

    @Override
    public FileApplyResponse fileApply(FileApplyRequest request) {
        // 1. 校验授权模式（AUTH-042/043：仅 local/site 支持授权文件申请）
        String mode = request.mode();
        if (!"local".equalsIgnoreCase(mode) && !"site".equalsIgnoreCase(mode)) {
            throw new LicenseException(ErrorCode.PARAM_001,
                    "授权文件申请仅支持 local/site 模式，当前模式：" + mode);
        }

        // 2. 生成并签名授权文件（AUTH-042/043：返回 license.infor 内容）
        String serial = "FA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("component", "Server");
        fields.put("version", request.edition() != null ? request.edition() : "1.0");
        fields.put("licensee", request.licensee() != null ? request.licensee() : "");
        fields.put("mode", mode.toLowerCase());
        fields.put("formal", "true");
        fields.put("expiration", request.expireAt() != null && !request.expireAt().isBlank()
                ? request.expireAt() : "never");
        fields.put("userinfor", request.remark() != null ? request.remark() : "");
        fields.put("proname", request.product() != null ? request.product() : "AS");
        fields.put("serial", serial);
        fields.put("center-required", "false");
        fields.put("max-instances", String.valueOf(request.maxInstances()));
        fields.put("max-cpus", "");
        fields.put("max-memory", "");
        String licenseFile = licenseFileGenerator.generate(fields);

        String applyId = serial;
        auditService.record(AuditOperationType.FILE_APPLY, "client", null,
                serial, "SUCCESS", "授权文件申请：" + mode.toLowerCase() + " 模式，序列号 " + serial);
        return new FileApplyResponse(
                applyId,
                mode.toLowerCase(),
                "SUCCESS",
                "已生成 " + mode.toLowerCase() + " 模式授权文件",
                licenseFile
        );
    }

    // ---- 私有辅助方法 ----

    private License findActiveLicense(String serial) {
        License license = licenseRepository.findBySerial(serial)
                .orElseThrow(() -> new LicenseException(ErrorCode.LICENSE_001, "授权不存在：" + serial));
        if (license.isDisabled()) {
            throw new LicenseException(ErrorCode.LICENSE_004, "授权已被禁用：" + serial);
        }
        if (license.isExpired() || license.isPastExpiration()) {
            throw new LicenseException(ErrorCode.LICENSE_004, "授权已过期：" + serial);
        }
        return license;
    }

    private RegisterResponse reRegister(Instance existing, License license, RegisterRequest request) {
        Instant now = Instant.now();
        Instance updated = new Instance(
                existing.id(),
                existing.instanceId(),
                license.id(),
                existing.clientUuid(),
                request.proname() != null ? request.proname() : existing.proname(),
                request.productType() != null ? request.productType() : existing.productType(),
                request.productVersion() != null ? request.productVersion() : existing.productVersion(),
                request.productSpec() != null ? request.productSpec() : existing.productSpec(),
                request.hostname() != null ? request.hostname() : existing.hostname(),
                request.ipAddress() != null ? request.ipAddress() : existing.ipAddress(),
                request.mac() != null ? request.mac() : existing.mac(),
                request.machineType() != null ? request.machineType() : existing.machineType(),
                request.currentCpus() != null ? request.currentCpus() : existing.currentCpus(),
                request.currentMemory() != null ? request.currentMemory() : existing.currentMemory(),
                existing.extendedAttributes(),
                Instance.STATUS_ONLINE,
                existing.onlineTime(),
                now,
                null,
                existing.createTime(),
                now
        );
        instanceRepository.save(updated);

        int interval = parseIntConfig(SystemConfigRepository.KEY_HEARTBEAT_INTERVAL, 30);
        return new RegisterResponse("SUCCESS", existing.instanceId(), Instance.STATUS_ONLINE, interval, "重注册成功");
    }

/**
     * 读取弹性配额倍数（req-27）。
     *
     * <p>来自 system_config.elastic.quota.multiplier，默认 2。非法值回退默认 2。</p>
     */
    private int getElasticQuotaMultiplier() {
        return parseIntConfig(SystemConfigRepository.KEY_ELASTIC_QUOTA_MULTIPLIER, 2);
    }

    /**
     * CPU/内存配额校验（AUTH-030/031 / LICENSE_003）。
     *
     * <p>当授权配置了 max_cpus / max_memory 且已用配额 + 请求配额超限时拒绝注册。
     * 校验在占用前执行，避免部分占用后回滚。</p>
     */
    private void validateCpuMemoryQuota(License license, RegisterRequest request) {
        Integer maxCpus = license.maxCpus();
        Integer maxMemory = license.maxMemory();
        Integer usedCpus = license.usedCpus() != null ? license.usedCpus() : 0;
        Integer usedMemory = license.usedMemory() != null ? license.usedMemory() : 0;

        if (maxCpus != null && maxCpus > 0 && request.currentCpus() != null) {
            if ((long) usedCpus + request.currentCpus() > maxCpus) {
                throw new LicenseException(ErrorCode.LICENSE_003,
                        "CPU 配额不足：已用 " + usedCpus + "，请求 " + request.currentCpus()
                                + "，上限 " + maxCpus);
            }
        }
        if (maxMemory != null && maxMemory > 0 && request.currentMemory() != null) {
            if ((long) usedMemory + request.currentMemory() > maxMemory) {
                throw new LicenseException(ErrorCode.LICENSE_003,
                        "内存配额不足：已用 " + usedMemory + "，请求 " + request.currentMemory()
                                + "，上限 " + maxMemory);
            }
        }
    }

    private void verifyRequestSignature(String canonical, String signature) {
        CommunicationSignature.verify(canonical, signature, communicationKeyProvider.getPublicKey());
    }

    private String buildRegisterCanonical(RegisterRequest request) {
        return join(request.serial(), request.clientUuid(), request.proname(),
                request.productType(), request.productVersion(), request.productSpec(),
                request.hostname(), request.ipAddress(), request.mac(), request.machineType());
    }

    private String buildHeartbeatCanonical(HeartbeatRequest request) {
        return join(request.instanceId(), request.serial());
    }

    private String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append(part == null ? "" : part);
        }
        return sb.toString();
    }

    private int parseIntConfig(String key, int defaultValue) {
        String value = systemConfigRepository.getValueOrDefault(key, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}