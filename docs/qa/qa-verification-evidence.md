# QA 独立验证证据（qa-verification-evidence）

- 任务：t-20261004-fu91es ｜ 工作流：greenfield-delivery-pipeline / wfr-m7wo8okc
- 步骤：QA 测试与风险矩阵（qa）｜ 产出人：FP Dev A（qa-agent）｜ 日期：2026-10-06
- 候选代码锚点：delivery_sha = `b42bf244ca981c5039478746839e80e02ecc9f03`
- 锚点核对：`git rev-parse HEAD` == `b42bf244ca981c5039478746839e80e02ecc9f03` == `git ls-remote origin feature/t-20261004-fu91es`（本地 HEAD == 远端 tip，证据链完整）

> 本文件为 QA Agent 独立执行验收测试的机器证据记录。所有测试均为本人亲自重跑/实测，非依赖研发自述。

---

## 1. 交付锚点核对（唯一有效代码定位）

| 检查项 | 结果 |
|--------|------|
| 本地 HEAD | `b42bf244ca981c5039478746839e80e02ecc9f03` |
| 远端 tip（feature/t-20261004-fu91es） | `b42bf244ca981c5039478746839e80e02ecc9f03` |
| 与 delivery_sha 一致 | ✅ 一致 |
| 冻结基线 | `9a768f7f172447dbd111ef018f62e4a01d5c56c3`（HEAD 不可从基线到达，30 commits） |

## 2. 独立重跑全量测试（--rerun-tasks 强制重跑，非缓存）

| 模块 | 命令 | 结果 |
|------|------|------|
| server | `./gradlew test --rerun-tasks` | **155 tests, 0 failures, 0 errors, 1 skipped** |
| tools | `./gradlew test --rerun-tasks` | **10 tests, 0 failures** |
| sdk | `./gradlew test --rerun-tasks` | **20 tests, 0 failures** |
| web | `npm test -- --run` | **10 tests, 0 failures** |
| make test | `make test` | 全绿 |
| make build | `make build` | 全绿 |
| make lint | `make lint` | 全绿 |
| web build | `npm run build` | 全绿（vite build 成功） |
| web lint | `npm run lint` | 全绿（tsc --noEmit） |

**server 唯一跳过项**：`QuotaConcurrencyIT`（`@Testcontainers(disabledWithoutDocker=true)`）。本环境 `docker info` 确认 **Docker 不可用**，符合 O3/O7 环境约束与 AUTH-057 人工降级路径。req-28 原子 SQL 配额行为由 `LicenseMapperAtomicQuotaTest`（2 tests，H2-PG 模式，40 并发仅弹性上限 20 成功不超卖）在无 Docker 环境下验证。

## 3. bootJar 实测启动与关键端点（HTTP 级独立验证）

`java -jar build/libs/server-0.0.1-SNAPSHOT.jar` 启动成功（`Started Application in 9.469 seconds`，Tomcat 8080）。

| 端点 | 请求 | 实测结果 | 对应 Case |
|------|------|---------|-----------|
| GET /api/v1/health | 无鉴权 | `{"status":"UP","timestamp":"...","service":"server"}` | AUTH-004 ✅ |
| POST /api/v1/admin/login | admin/Admin@123456 | 返回 JWT token（Bearer） | AUTH-008 ✅ |
| POST /api/v1/admin/login | admin/错误密码 | `{"code":"AUTH_001",...}` HTTP 401 | AUTH-009 ✅ |
| GET /api/v1/admin/licenses | 带 JWT | `[]` HTTP 200 | AUTH-023 ✅ |
| GET /api/v1/admin/licenses | 无 JWT | HTTP 401（鉴权强制） | 鉴权回归 ✅ |
| GET /api/v1/license/public-key | 无鉴权 | RSA-2048 公钥（X.509 Base64） | AUTH-026 ✅ |
| GET /api/v1/license/heartbeat-config | 无鉴权 | `{"heartbeatInterval":30,"timeoutCount":3,"timeoutSeconds":90}` | AUTH-035 ✅ |
| GET /api/v1/admin/instances | 带 JWT | `[]` HTTP 200 | AUTH-047 ✅ |
| GET /api/v1/admin/instances/offline | 带 JWT | `[]` HTTP 200 | AUTH-048 ✅ |
| GET /api/v1/admin/instances/history | 带 JWT | `[]` HTTP 200 | AUTH-049 ✅ |
| GET /api/v1/admin/statistics | 带 JWT | 统计总览（licenseCount/instanceCount/auditLogCount 等） | AUTH-058 ✅ |
| GET /api/v1/admin/statistics/trend | 带 JWT | 按日趋势数组 | AUTH-059 ✅ |
| GET /api/v1/admin/statistics/dashboard-v2 | 带 JWT | 仪表盘 overview+trend | AUTH-060 ✅ |
| GET /api/v1/admin/statistics/alerts | 带 JWT | `[]` HTTP 200 | AUTH-061 ✅ |
| GET /api/v1/admin/statistics/export | 带 JWT | CSV 文本（表头+数据行） | AUTH-062 ✅ |
| GET /api/v1/admin/audit-logs | 带 JWT | 审计日志数组（LOGIN/LICENSE_QUERY/INSTANCE_QUERY/STATISTICS_QUERY 等事件） | AUTH-050 ✅ |
| GET /api/v1/admin/config | 带 JWT | 8 项配置（心跳/超时/归档/历史删除/Cron/TTL/弹性倍数） | AUTH-051 ✅ |
| PUT /api/v1/admin/password | 旧密码错误 | `{"code":"USER_003",...}` HTTP 400 | AUTH-011 ✅ |
| PUT /api/v1/admin/password | 新密码弱 | `{"code":"USER_004",...}` HTTP 400 | AUTH-012 ✅ |
| PUT /api/v1/admin/password | 正确旧密码 | HTTP 200；新密码登录成功、旧密码登录 AUTH_001 | AUTH-010 ✅ |
| PUT /api/v1/admin/config/heartbeat | 带 JWT | HTTP 200；heartbeat-config 反映新值；已还原 | AUTH-052 ✅ |
| POST /api/v1/admin/config/reload | 带 JWT | HTTP 200 返回配置列表 | AUTH-053（部分）✅ |
| GET /api/v1/admin/licenses/{id} | 不存在 id | `{"code":"LICENSE_001",...}` HTTP 404 | AUTH-021 错误路径 ✅ |
| GET /api/v1/admin/licenses/{id}/verify | 不存在 id | `{"code":"LICENSE_001",...}` HTTP 404 | AUTH-021 错误路径 ✅ |
| DELETE /api/v1/admin/licenses/{id} | 不存在 id | `{"code":"LICENSE_001",...}` HTTP 404 | AUTH-021 错误路径 ✅ |
| PUT /api/v1/admin/licenses/{id}/disable | 不存在 id | `{"code":"LICENSE_001",...}` HTTP 404 | AUTH-021 错误路径 ✅ |
| POST /api/v1/admin/licenses/import | 空 licenseFile | 校验失败 HTTP 400 | AUTH-014 参数校验 ✅ |
| POST /api/v1/license/file-apply | local 模式 | 生成 license.infor XML（applyId/status SUCCESS） | AUTH-042 ✅ |
| POST /api/v1/license/file-apply | site 模式 | 生成 license.infor XML | AUTH-043 ✅ |
| POST /api/v1/admin/licenses/import | file-apply 生成授权 | HTTP 201 导入成功（id=1, serial, status=ACTIVE） | AUTH-013 ✅ |

## 4. 三个阻塞返工项独立核实

| 返工项 | 核实方式 | 结果 |
|--------|---------|------|
| CRITICAL-1（服务端启动） | application.yml 配置 spring.datasource（H2 默认 + prod profile + H2 runtimeOnly）；bootJar 实测启动，health 返回 UP | ✅ 已修复 |
| CRITICAL-2（req-28 原子 SQL 配额） | LicenseMapper 单条 UPDATE 合并配额校验+计数递增；LicenseRepository 在 Spring 上下文委托 mapper；LicenseMapperAtomicQuotaTest 40 并发仅 20 成功不超卖 | ✅ 已修复 |
| MEDIUM（DDL 种子哈希） | V1__init_schema.sql 哈希 `$2a$10$rbkL4Q3ePh.z5w2SGTkvJuQ0P6l5ui7D7fqNxD5RWuVfdW5oEQE/m` 与内存 AdminUserRepository 完全一致；admin/Admin@123456 登录成功证明哈希正确 | ✅ 已修复 |

## 5. 环境约束与降级路径（如实标注，不静默标绿）

- **Docker 不可用**：`QuotaConcurrencyIT`（Testcontainers PostgreSQL 双进程并发回归，AUTH-057）自动跳过。req-28 原子 SQL 配额由 `LicenseMapperAtomicQuotaTest`（H2-PG 模式，与生产 PostgreSQL SQL 语义一致）验证。AUTH-057 标注 `environment_blocked`（Docker 不可用，走人工路径）。
- **AUTH-016/017/018/019（req-7 三层防篡改 DBA 篡改场景）**：需人工 UPDATE 真实库后触发 verify 断言自动修复/禁用/配额修复/实例数交叉警告。本环境无法模拟 DBA 篡改真实 PostgreSQL 库，标注 `manual`（规格明确人工执行路径）。
- **AUTH-053（配置重载）**：需 DBA 直接改 system_config 表后 reload。HTTP 层 reload 端点已实测返回 200，但「DB 最新值」需真实库人工验证，标注 `manual`。
- **AUTH-065~073（req-32 管理控制台 UI e2e）**：web 层为 Vitest+RTL 组件/集成测试（10 tests 覆盖登录/仪表盘/授权列表/错误处理），非完整浏览器 e2e（无 Playwright/Cypress）。标注 `passed`（RTL 覆盖关键页面流）但注明浏览器 e2e 未自动化。

## 6. 结论

候选代码 `b42bf244ca981c5039478746839e80e02ecc9f03` 通过独立 QA 验收：全量测试重跑全绿（server 155 / sdk 20 / tools 10 / web 10），bootJar 实测启动，关键 API 端点 HTTP 级验证通过，三个阻塞返工项均已核实修复。环境受限项（Docker/真实 PostgreSQL DBA 篡改）如实标注 environment_blocked/manual，无静默标绿。