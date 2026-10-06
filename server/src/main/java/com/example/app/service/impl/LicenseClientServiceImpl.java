package com.example.app.service.impl;

import com.example.app.exception.ErrorCode;
import com.example.app.exception.LicenseException;
import com.example.app.license.CommunicationKeyProvider;
import com.example.app.license.CommunicationSignature;
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
import com.example.app.service.LicenseClientService;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

    public LicenseClientServiceImpl(CommunicationKeyProvider communicationKeyProvider,
                                    LicenseRepository licenseRepository,
                                    InstanceRepository instanceRepository,
                                    SystemConfigRepository systemConfigRepository) {
        this.communicationKeyProvider = communicationKeyProvider;
        this.licenseRepository = licenseRepository;
        this.instanceRepository = instanceRepository;
        this.systemConfigRepository = systemConfigRepository;
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

        // 3. 已存在实例则执行重注册（req-17）
        String instanceId = request.clientUuid() != null && !request.clientUuid().isBlank()
                ? request.clientUuid()
                : "inst-" + UUID.randomUUID().toString().substring(0, 8);

        Instance existing = instanceRepository.findByInstanceId(instanceId).orElse(null);
        if (existing != null) {
            return reRegister(existing, license, request);
        }

        // 4. 配额判定（INSTANCE_004 / INSTANCE_001）
        checkQuota(license);

        // 5. 创建实例记录
        Instant now = Instant.now();
        Instance instance = new Instance(
                null,
                instanceId,
                license.id(),
                request.clientUuid(),
                request.proname() != null ? request.proname() : license.proname(),
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

        // 6. 更新授权配额计数
        incrementUsedInstances(license);

        int interval = parseIntConfig(SystemConfigRepository.KEY_HEARTBEAT_INTERVAL, 30);
        return new RegisterResponse(instanceId, Instance.STATUS_ONLINE, interval, "注册成功");
    }

    @Override
    public HeartbeatResponse heartbeat(HeartbeatRequest request) {
        // 1. 校验通信签名（LICENSE_002）
        verifyRequestSignature(buildHeartbeatCanonical(request), request.signature());

        // 2. 查找实例（INSTANCE_002）
        Instance instance = instanceRepository.findByInstanceId(request.instanceId())
                .orElseThrow(() -> new LicenseException(ErrorCode.INSTANCE_002,
                        "实例不存在：" + request.instanceId()));

        // 3. 更新心跳时间
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

        return new HeartbeatResponse(request.instanceId(), Instance.STATUS_ONLINE, now.toString(), "心跳成功");
    }

    @Override
    public FileApplyResponse fileApply(FileApplyRequest request) {
        // 1. 校验通信签名（LICENSE_002）
        verifyRequestSignature(buildFileApplyCanonical(request), request.signature());

        // 2. 查找授权（LICENSE_001）
        License license = findActiveLicense(request.serial());

        // 3. 仅 local/site 模式支持授权文件申请
        String mode = license.licenseMode();
        if (!"local".equalsIgnoreCase(mode) && !"site".equalsIgnoreCase(mode)) {
            throw new LicenseException(ErrorCode.PARAM_001,
                    "授权文件申请仅支持 local/site 模式，当前模式：" + mode);
        }

        return new FileApplyResponse(
                license.serial(),
                license.bxbFile(),
                mode,
                "授权文件申请成功"
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
        return new RegisterResponse(existing.instanceId(), Instance.STATUS_ONLINE, interval, "重注册成功");
    }

    private void checkQuota(License license) {
        int maxInstances = license.maxInstances() != null ? license.maxInstances() : 0;
        int usedInstances = license.usedInstances() != null ? license.usedInstances() : 0;
        if (maxInstances > 0 && usedInstances >= maxInstances * 2) {
            throw new LicenseException(ErrorCode.INSTANCE_004, "配额超 200% 上限，拒绝注册");
        }
    }

    private void incrementUsedInstances(License license) {
        int used = license.usedInstances() != null ? license.usedInstances() : 0;
        int remaining = license.remainingInstances() != null ? license.remainingInstances() : 0;
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
                used + 1,
                Math.max(0, remaining - 1),
                license.usedCpus(),
                license.usedMemory(),
                license.bxbFile(),
                license.status(),
                license.source(),
                license.createTime(),
                Instant.now()
        );
        licenseRepository.save(updated);
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

    private String buildFileApplyCanonical(FileApplyRequest request) {
        return join(request.serial(), request.clientUuid(), request.proname(),
                request.productType(), request.productVersion(), request.hostname(),
                request.ipAddress(), request.mac());
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