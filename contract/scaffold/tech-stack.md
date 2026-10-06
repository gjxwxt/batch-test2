# 技术栈脚手架契约 (Tech Stack Scaffold Contract)

> 共享契约条目：`infra:scaffold`
> 承载基线 commit：`448ca97f482f6593bac1321a3f7827027c0921a4`
> 分支：`feature/t-20261004-fu91es`

本文档冻结 IAS 授权中心全栈技术选型与测试环境约定。所有工作包必须在此技术栈内实现。

## 1. 后端 (Backend)

- **语言 / 运行时**：Java 21
- **框架**：Spring Boot 3.3.3
- **构建**：Gradle
- **ORM**：MyBatis-Plus
- **数据库**：PostgreSQL 12+
- **迁移**：Flyway（迁移脚本位于 `server/src/main/resources/db/migration/`）

### 分层约束
严格分层：`controller/` → `service/` → `repository/` → `model/`。
- Controller 仅做参数校验、调用 Service、返回 DTO 与 HTTP 状态码。
- 业务规则收敛在 Service。
- 失败统一抛领域异常，由 `@RestControllerAdvice` 转换为 `ApiErrorResponse`。

## 2. 前端 (Frontend)

- **框架**：React 18
- **语言**：TypeScript
- **构建**：Vite
- **样式**：Tailwind CSS

### 分层约束
- `components/`：无状态或受控 UI 组件。
- `pages/`：视图路由页面。
- `services/api.ts`：所有 HTTP 请求收敛于此，强类型入参/出参。
- `types/`：与后端 DTO 对齐的 TypeScript 类型。

## 3. 测试环境 (Test Environment)

- **单元测试**：H2 数据库 PostgreSQL 兼容模式。
- **集成测试**：Testcontainers PostgreSQL 12+（双节点并发，O3/O7 场景）。

## 4. 交付基线

- 后端测试：14/14 通过。
- 前端测试：7/7 通过。