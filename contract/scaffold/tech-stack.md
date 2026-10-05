# 共享契约 · 技术栈骨架（infra:scaffold）

> 冻结基线（wp-0）。技术栈与模块结构为所有并行工作包的构建接缝。
> 需求来源：`center模式-服务端需求文档(1).md` 1.2 系统概述 + 需求澄清稿 §5 关键约束（O1/O3/O5）。

## 技术栈（O1 裁决）

| 层 | 技术 | 说明 |
|----|------|------|
| 服务端 | Spring Boot 3.3.3 / Java 21 / Gradle | 分层 controller→service→repository→model |
| ORM | MyBatis-Plus | 数据库访问（PostgreSQL 12+） |
| 数据库 | PostgreSQL 12+ | 多节点共享，配额计数以数据库为准 |
| 前端 | React 18 + TypeScript + Vite + Tailwind | 管理控制台 |
| 测试 | JUnit 5 + MockMvc + Testcontainers | 单测内存库/嵌入 mock；双节点并发用 Testcontainers PG 双进程（O3/O7） |

## 模块结构（O5 裁决）

授权生成工具与客户端 SDK 以**独立 Gradle 模块 + 可执行/可嵌入 JAR** 产出，与服务端进程生命周期解耦：

```
server/                     # 授权中心服务端（Spring Boot）
  ├── src/main/java/...     # controller / service / repository / model / exception / config
  └── src/main/resources/db/migration/   # Flyway 迁移（V1__init_schema.sql）
tools/                      # 授权生成工具（独立 Gradle 模块，可执行 JAR）
sdk/                        # 客户端 SDK（独立 Gradle 模块，可嵌入 JAR）
web/                        # 管理控制台前端（React 18 + TS + Vite + Tailwind）
contract/                   # 共享契约（DDL / 错误码 / API 骨架 / 签名规范 / 技术栈）
```

## 分层与编码规则

- 严格分层：`controller/` → `service/` → `repository/` → `model/`。
- Java 21 `record` 用于 DTO 与 API 请求/响应。
- 统一 `ApiErrorResponse`（infra:error-codes）通过 `@RestControllerAdvice` 返回。
- Controller 只校验输入、调用 service、返回 DTO 与正确 HTTP 状态码。
- 前端所有网络请求走 `services/api.ts`，带 TypeScript 类型。
- **TDD 先行**：先写失败测试验证验收标准，再实现业务代码。

## 变更控制

技术栈与模块结构为冻结契约。新增依赖/模块须经 contract_review 评审。