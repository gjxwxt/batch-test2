package com.example.app.model;

import java.time.Instant;

/**
 * 授权列表项（IAS_AUTH_LIST）。
 *
 * <p>列表视图不暴露完整授权文件（bxb_file），仅返回概要字段。</p>
 */
public record LicenseSummary(
        Long id,
        String serial,
        String licenseName,
        String proname,
        String component,
        String version,
        String licensee,
        String licenseMode,
        String formal,
        String expiration,
        String status,
        Integer maxInstances,
        Integer usedInstances,
        Integer remainingInstances,
        Instant createTime,
        Instant updateTime
) {

    public static LicenseSummary from(License license) {
        return new LicenseSummary(
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
                license.status(),
                license.maxInstances(),
                license.usedInstances(),
                license.remainingInstances(),
                license.createTime(),
                license.updateTime()
        );
    }
}