package com.example.app.model;

/**
 * 审计事件类型（13 种）。
 * 覆盖认证中心管理端与授权/实例/配置/统计等全部关键操作。
 */
public enum AuditOperationType {
    /** 管理员登录 */
    LOGIN,
    /** 修改密码 */
    CHANGE_PASSWORD,
    /** 导入授权 */
    LICENSE_IMPORT,
    /** 删除授权 */
    LICENSE_DELETE,
    /** 禁用授权 */
    LICENSE_DISABLE,
    /** 授权防篡改校验 */
    LICENSE_VERIFY,
    /** 授权查询 */
    LICENSE_QUERY,
    /** 实例下线 */
    INSTANCE_OFFLINE,
    /** 实例查询 */
    INSTANCE_QUERY,
    /** 系统配置更新 */
    CONFIG_UPDATE,
    /** 系统配置重载 */
    CONFIG_RELOAD,
    /** 审计日志查询 */
    AUDIT_QUERY,
    /** 统计查询 */
    STATISTICS_QUERY
}