# 共享契约 · 6 表 DDL（infra:ddl）

> 冻结基线（wp-0）。所有并行工作包以此为数据库接缝。目标库：PostgreSQL 12+（多节点共享）。
> 可执行迁移：`server/src/main/resources/db/migration/V1__init_schema.sql`（Flyway）。
> 需求来源：`center模式-服务端需求文档(1).md` 6.3 节 + 各 Use Case 数据需求。

## 表清单

| 表 | 用途 | 涉及 Use Case |
|----|------|--------------|
| admin_user | 管理员账号 | LOGIN / CHANGE_PWD |
| license | 授权 | IMPORT / LIST / DETAIL / DELETE / DISABLE / TAMPER_CHECK |
| instance | 在线实例 | REGISTER / HEARTBEAT / TIMEOUT |
| history_instance | 历史实例（归档） | ARCHIVE / HISTORY_DELETE |
| audit_log | 审计日志 | AUDIT |
| system_config | 系统配置（8 项） | CONFIG_VIEW / CONFIG_HB |

## 关键字段约定

- **license**：`serial` 全局唯一（UUID）；`status` ∈ ACTIVE/DISABLED/EXPIRED；`license_mode` ∈ center/local/site；`used_instances`/`remaining_instances`/`used_cpus`/`used_memory` 为配额计数，**以数据库为准**（req-28 强一致）。
- **instance**：`instance_id` 客户端唯一实例标识；`status` ∈ ONLINE/OFFLINE；`last_heartbeat_time` 用于超时检测（req-18）。
- **history_instance**：镜像 instance 结构 + `offline_time`/`archived_time`，归档迁移（req-19）。
- **audit_log**：`operation_type` 覆盖 13 种审计事件（req-24）；`result` ∈ SUCCESS/FAILURE/WARNING。
- **system_config**：8 项配置（4.10.1.1），`config_key` 唯一。

## 种子数据（O4 裁决）

- 初始管理员：`admin` / `Admin@123456`（BCrypt 哈希），status=ACTIVE。首登强制改密不在本次范围。
- 8 项系统配置默认值（心跳间隔 30s、超时倍数 3、归档 30 天、历史删除 90 天、归档/过期 Cron、令牌 TTL 120 分钟、弹性配额警告倍数 2）。

## 变更控制

本文件与 `V1__init_schema.sql` 为冻结契约。任何表结构变更须经 contract_review 评审后以增量迁移（V2__…）落地，不得修改已冻结的 V1。