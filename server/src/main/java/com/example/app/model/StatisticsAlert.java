package com.example.app.model;

/**
 * 统计告警 DTO（GET /api/v1/admin/statistics/alerts —— IAS_AUTH_ALERTS）。
 *
 * <p>管理端需要关注的异常项：即将过期授权、配额告警、离线实例等。</p>
 *
 * @param level   告警级别（WARN / CRITICAL）
 * @param type    告警类型（LICENSE_EXPIRING / QUOTA_WARNING / INSTANCE_OFFLINE）
 * @param message 告警描述
 * @param target  关联对象标识（授权序列号 / 实例 ID 等）
 */
public record StatisticsAlert(
        String level,
        String type,
        String message,
        String target
) {

    public static final String LEVEL_WARN = "WARN";
    public static final String LEVEL_CRITICAL = "CRITICAL";

    public static final String TYPE_LICENSE_EXPIRING = "LICENSE_EXPIRING";
    public static final String TYPE_QUOTA_WARNING = "QUOTA_WARNING";
    public static final String TYPE_INSTANCE_OFFLINE = "INSTANCE_OFFLINE";
}