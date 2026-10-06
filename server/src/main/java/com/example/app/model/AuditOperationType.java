package com.example.app.model;

/**
 * 审计事件类型。
 *
 * <p>覆盖认证中心管理端与授权/实例/配置/统计等全部关键操作。
 * 核心 13 种事件（AUTH-050）：LOGIN / CHANGE_PASSWORD / LICENSE_IMPORT / LICENSE_DELETE /
 * LICENSE_DISABLE / LICENSE_VERIFY / LICENSE_QUERY / INSTANCE_OFFLINE / INSTANCE_QUERY /
 * CONFIG_UPDATE / CONFIG_RELOAD / AUDIT_QUERY / STATISTICS_QUERY。
 * 另含客户端交互与实例生命周期事件（REGISTER / HEARTBEAT / FILE_APPLY / TIMEOUT /
 * ARCHIVE / INSTANCE_DELETE / INSTANCE_RECOVER），以满足 AUTH-027/028/038/042-046 的审计要求。</p>
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
    STATISTICS_QUERY,
    /** 实例注册 */
    REGISTER,
    /** 客户端心跳 */
    HEARTBEAT,
    /** 授权文件申请 */
    FILE_APPLY,
    /** 心跳超时检测 */
    TIMEOUT,
    /** 实例归档 */
    ARCHIVE,
    /** 历史实例删除 */
    INSTANCE_DELETE,
    /** 实例下线恢复 */
    INSTANCE_RECOVER
}