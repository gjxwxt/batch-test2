# 共享契约（Shared Contract）— wp-0

> 冻结基线。所有并行工作包（wp-1~wp-10）以此为唯一共享面。
> 契约先行：wp-0 先冻结并提交基线，其余工作包按依赖接缝并行。

## 契约清单

| 契约 | 目录 | 说明 |
|------|------|------|
| 6 表 DDL | `ddl/6-tables-ddl.md` | admin_user / license / instance / history_instance / audit_log / system_config + 种子账号 |
| 错误码目录 | `error-codes/error-codes.md` | 统一 ApiErrorResponse 错误码枚举 |
| API 骨架 | `api/api-skeleton.md` | 6.2 映射路径 / 鉴权 / 错误语义 |
| 签名 canonical 规范 | `signature/signature-canonical.md` | RSA-2048 + SHA256withRSA + 盐值 |
| 技术栈骨架 | `scaffold/tech-stack.md` | Spring Boot 3.3.3 / Java 21 / MyBatis-Plus / React 18 |

## 可执行载体

- 数据库迁移：`server/src/main/resources/db/migration/V1__init_schema.sql`（Flyway）
- 错误码枚举：`server/src/main/java/com/example/app/exception/ErrorCode.java`
- API 骨架：`server/src/main/java/com/example/app/controller/*Controller.java`

## 变更控制

本目录与上述可执行载体为冻结契约。任何变更须经 contract_review 评审，以增量方式落地（如 V2__… 迁移），不得修改已冻结基线。