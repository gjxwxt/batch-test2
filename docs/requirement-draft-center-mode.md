# IAS Auth Center（Center 模式）需求澄清稿（requirement_draft）

- 任务：t-20261004-fu91es ｜ 工作流：greenfield-delivery-pipeline / wfr-m7wo8okc
- 步骤：需求澄清（requirement_draft）｜ 产出人：FP Dev A（developer）｜ 日期：2026-10-05（澄清轮返工 v2）
- 需求基线：`center模式-服务端需求文档(1).md`（sha256:a26737e67d013870aa5e7cefe59e3952b9e9ee847ad62d35eb8c7535b82a1346）
- 交付计划草案：preplan-ias-auth-center-mode（随任务下发，11 个工作包 wp-0~wp-10，需求锚点 req-1~req-33）
- 澄清轮次：review_rounds=1（本轮 7 项澄清 O1~O7 全部已答复，open_questions 清零）

> **本轮修订说明（v2）**：7 项澄清（O1~O7）均已答复并作为**裁决**织入正文，不再作为待定项。其中 O2「带着吧」将 4.8 统计与仪表盘、4.11.1 local/site 授权申请**纳入本次交付**，相应新增需求锚点 req-34~req-37 并调整工作包 wp-4/wp-8。open_questions 已清零。

## 1. 问题（原始请求与业务背景）

InforSuite AS 过去依赖本地 `license.infor` 文件做单机授权校验（RSA-2048 + SHA256 验签、有效期、硬件绑定）。在数据中心大规模部署（几十到几百个 AS 实例、容器化/虚拟化/K8S）场景下暴露四个核心问题：

1. **配额失控**：IP 漂移使旧硬件绑定机制无法限制授权使用；
2. **多实例管理困难**：数十上百实例无法统一管控；
3. **缺乏合规审计**：金融/党政军客户要求记录"谁在何时启动了实例""授权使用情况""异常注册尝试"；
4. **动态运维无支撑**：临时扩缩容缺少中心化实时视图。

## 2. 目标

引入 IAS Auth Center **center 模式**，本次交付覆盖其全链路：

1. Center 模式全链路：授权签发 → 导入 → 实例注册 → 心跳保活 → 超时下线 → 实例归档 → 重注册（req-33）；
2. 弹性配额三级判定：≤max 放行；max<used≤2×max 警告仍放行；>2×max 拒绝（req-27）；
3. 多节点部署配额强一致：原子 SQL/行级锁 + CAS 条件更新，不超卖、不负数（req-28）；
4. 三层防篡改校验：签名 → 字段一致性自动修复 → 配额守恒 + 在线实例交叉验证（req-7）；
5. 合规审计：13 种审计事件全覆盖（req-24）；
6. 管理控制台：登录/密码、授权导入/列表/详情/禁用/删除、实例监控、审计查询、系统配置、**统计概览/趋势/仪表盘与告警**、统计导出；
7. **local/site 授权文件申请**（统一掌握各模式授权使用情况，便于管理与审计）；
8. 仅用心跳超时判定下线，不提供主动下线接口（决策 2）。

## 3. 范围（交付物 4 件套 + 共享契约，11 个工作包）

| 交付物 | 内容 | 对应工作包 |
|---|---|---|
| 授权生成工具 | RSA 密钥对生成、center 模式 license.infor 与授权池签发 | wp-1 |
| 授权中心服务端 | 启动自检/初始化、认证、授权管理、注册/心跳、超时/归档、监控、审计、配置、弹性配额、多节点一致性、**local/site 授权文件申请**、统计概览/趋势/仪表盘与告警、统计导出 | wp-2 ~ wp-8 |
| 客户端 SDK | 本地校验 → 公钥 → 注册 → 心跳配置 → 心跳循环 → 重注册，center-required 策略；**local/site 模式授权文件申请** | wp-9 |
| 管理控制台前端 | 登录、授权管理、实例监控、审计、配置、**统计报表/仪表盘**等管理页面 | wp-10 |
| 共享契约 + 集成验收 | 6 表 DDL、错误码、API 骨架、签名 canonical 规范冻结；端到端集成 | wp-0 |

需求域：文档 4.1~4.11 全量（含 4.8 统计与仪表盘、4.11.1 local/site 授权文件申请），及 6.2 表中历史实例查询与配置 reload 接口；细化为 req-1~req-37 共 37 个实现锚点。验证方法按 5.1 节逐用例测试（全部 T）。

> **范围裁决（O2）**：4.8 统计概览/趋势/仪表盘与告警、4.11.1 local/site 授权文件申请**纳入本次交付**（裁决「带着吧」），不再作为非目标。

## 4. 非目标（明确排除）

- 不提供主动下线接口；local/site 模式的**完整注册/心跳机制**不在本次范围（仅提供授权文件申请，见 req-37）；
- 既有 local 模式行为改造不涉及（存量兼容不在本次验收内）；
- 首登强制改密属生产要求，本次范围外（采用种子账号，见 O4 裁决）。

## 5. 关键约束

- **技术栈（O1 裁决）**：沿用仓库脚手架——服务端 **Spring Boot 3.3.3 / Java 21 / Gradle + MyBatis-Plus**，前端 **React 18 + TypeScript + Vite + Tailwind**；分层 controller→service→repository→model、record DTO、ApiErrorResponse 统一异常、`services/api.ts` 统一网络层、TDD 先行。文档（Vue3/SB4.0.6）仅作为**接口与行为规格**，页面功能按 4.x 界面示意实现，API 按 6.2 映射。
- **测试环境（O3 裁决）**：CI 无外置 PostgreSQL——单测用内存库/嵌入 mock，双节点并发集成用 **Testcontainers（PostgreSQL 12+ 镜像）**；配额一致性（req-28）必须以真实 PG 并发语义验证，H2 等内存库不能替代原子 SQL/CAS 行为。
- **初始账号（O4 裁决）**：DDL 内置 `admin/Admin@123456` 初始账号作为验收前置，审计记录初始化事件；首登强制改密不在本次范围。
- **交付形态（O5 裁决）**：授权生成工具与客户端 SDK 以**独立 Gradle 模块 + 可执行/可嵌入 JAR** 产出（server/ 下多模块或并列目录），与服务端进程生命周期解耦。
- **双密钥（O6 裁决）**：签发工具持有的「授权签名 RSA 密钥对」（4.1.1）与服务端「通信 RSA 密钥对」（4.5.1）确认为**两套独立密钥、互不复用**——前者签 license 文件、服务端只持公钥验签；后者签注册/心跳请求与公钥分发响应。
- **多节点验收环境（O7 裁决）**：双节点并发注册不超卖（req-28）为核心回归，验收环境认可**同库双实例部署（Testcontainers 双进程）**复现跨节点竞态。
- 配额一致性以数据库为准；心跳时间维护在内存并批量落库；6 张共享表 admin_user / license / instance / history_instance / audit_log / system_config（6.3 节）。
- API 路径以 6.2 映射表为准（/api/v1/admin/**、/api/v1/license/**、/api/v1/health），含新增统计与 file-apply 端点。

## 6. 风险

| # | 风险 | 应对 |
|---|---|---|
| R1 | 多节点配额超卖/负数（并发注册、重复下线、恢复重扣） | wp-0 冻结原子 SQL 契约；双节点并发注册为回归必测项（qaPolicy，Testcontainers 双进程，O7） |
| R2 | 禁用/删除/历史清理为不可逆操作，误实现不可恢复 | TDD 覆盖幂等与级联断言；审计事件逐条验证 |
| R3 | 防篡改三层校验存在"自动修复"路径，可能掩盖真实篡改 | 层②修复必审计 + 层③交叉验证告警/报错；DBA 篡改场景列为核心回归 |
| R4 | 心跳/超时/归档依赖时钟与调度，测试耗时长 | 心跳参数可配置（10~86400s、倍数 2~10）、归档天数支持小数（5.1/4.10.2） |
| R5 | 签名盐值（InforSuiteAuth2026 等）与密钥存储（XOR+Base64）强度有限 | 按文档实现即可（安全基线由文档定义），风险记录在案 |
| R6 | 工作流为单管线（greenfield），但计划含 11 个并行工作包 | 契约先行（wp-0 先冻结），实现步骤内按 sharedContract 接缝 |
| R7 | 统计/仪表盘聚合查询（license/instance/history_instance）在数据量大时性能 | 按 4.8 时间粒度聚合（分钟/小时/天），查询走索引；列为性能回归项 |

## 7. 待澄清问题（open_questions）

> 本轮 7 项澄清（O1~O7）均已答复并织入正文（见 §3/§5），**open_questions 已清零**。无待定项。

## 8. 需求锚点（req-1~req-37）

`requirement_items`（req-1~req-37）取自交付计划草案 preplan-ias-auth-center-mode 并补充 O2 纳入项（req-34~req-37），id/text/source 随本步骤提交评审冻结；所有非基础设施工作包的 acceptanceCriteria 必须引用这些 id。

| 锚点 | 需求 | 来源 Use Case | 工作包 |
|------|------|--------------|--------|
| req-1 | 授权文件生成（center 模式） | IAS_AUTH_GENERATE | wp-1 |
| req-2 | 服务端启动自检 | IAS_AUTH_SELF_CHECK | wp-2 |
| req-3 | 服务端启动初始化 | IAS_AUTH_INIT | wp-2 |
| req-4 | 管理员登录 | IAS_AUTH_LOGIN | wp-2 |
| req-5 | 修改管理员密码 | IAS_AUTH_CHANGE_PWD | wp-2 |
| req-6 | 授权导入 | IAS_AUTH_IMPORT | wp-3 |
| req-7 | 三层防篡改校验 | IAS_AUTH_TAMPER_CHECK | wp-3 |
| req-8 | 授权删除 | IAS_AUTH_DELETE | wp-3 |
| req-9 | 授权禁用 | IAS_AUTH_DISABLE | wp-3 |
| req-10 | 授权列表查询 | IAS_AUTH_LIST | wp-3 |
| req-11 | 授权详情查看 | IAS_AUTH_DETAIL | wp-3 |
| req-12 | RSA 公私钥对生成 | IAS_AUTH_KEYPAIR_INIT | wp-4 |
| req-13 | RSA 公钥分发 | IAS_AUTH_PUBLIC_KEY | wp-4 |
| req-14 | 实例注册 | IAS_AUTH_REGISTER | wp-4 |
| req-15 | 心跳参数获取 | IAS_AUTH_HB_CONFIG | wp-4 |
| req-16 | 客户端心跳 | IAS_AUTH_HEARTBEAT | wp-4 |
| req-17 | 重注册 | IAS_AUTH_REREGISTER | wp-4 |
| req-18 | 心跳超时检测与下线 | IAS_AUTH_TIMEOUT | wp-5 |
| req-19 | 实例归档 | IAS_AUTH_ARCHIVE | wp-5 |
| req-20 | 历史实例删除 | IAS_AUTH_HISTORY_DELETE | wp-5 |
| req-21 | 在线实例查询 | IAS_AUTH_INST_LIST | wp-5 |
| req-22 | 下线实例查询 | IAS_AUTH_INST_OFFLINE | wp-5 |
| req-23 | 实例详情查看 | IAS_AUTH_INST_DETAIL | wp-5 |
| req-24 | 审计日志查询 | IAS_AUTH_AUDIT | wp-6 |
| req-25 | 系统配置查看 | IAS_AUTH_CONFIG_VIEW | wp-6 |
| req-26 | 心跳配置修改 | IAS_AUTH_CONFIG_HB | wp-6 |
| req-27 | 弹性配额判定 | IAS_AUTH_ELASTIC_QUOTA | wp-7 |
| req-28 | 多节点配额一致性 | IAS_AUTH_MULTI_NODE | wp-7 |
| req-29 | 统计导出 | IAS_AUTH_STAT_EXPORT | wp-8 |
| req-30 | 客户端 SDK 本地校验 | IAS_AUTH_SELF_CHECK（SDK 侧） | wp-9 |
| req-31 | 客户端 SDK 注册/心跳/重注册生命周期 | IAS_AUTH_REGISTER/HEARTBEAT/REREGISTER（SDK 侧） | wp-9 |
| req-32 | 管理控制台管理页面 | 各 admin 用例前端 | wp-10 |
| req-33 | Center 模式全链路集成 | 全链路 | wp-0/wp-10 |
| req-34 | 统计概览 | IAS_AUTH_STAT_OVERVIEW | wp-8 |
| req-35 | 趋势数据 | IAS_AUTH_STAT_TREND | wp-8 |
| req-36 | 仪表盘与告警 | IAS_AUTH_DASHBOARD | wp-8 |
| req-37 | 授权文件申请（local/site） | IAS_AUTH_FILE_APPLY | wp-4/wp-9 |

## 9. 工作包拆分（11 个工作包）

| 工作包 | 名称 | 内聚域 | 覆盖 Use Case | 依赖 |
|--------|------|--------|--------------|------|
| wp-0 | 共享契约与基础设施 | 6 表 DDL、错误码、API 骨架、签名 canonical 规范、构建脚手架 | 全部（契约） | — |
| wp-1 | 授权生成工具 | 授权签发 | IAS_AUTH_GENERATE | wp-0 |
| wp-2 | 服务端启动与认证 | 启动自检/初始化、管理员认证 | SELF_CHECK, INIT, LOGIN, CHANGE_PWD | wp-0 |
| wp-3 | 授权管理 | 授权导入/校验/删除/禁用/查询 | IMPORT, TAMPER_CHECK, DELETE, DISABLE, LIST, DETAIL | wp-0, wp-2 |
| wp-4 | 客户端交互（注册/心跳/文件申请） | 通信密钥、注册、心跳、重注册、local/site 授权文件申请 | KEYPAIR_INIT, PUBLIC_KEY, REGISTER, HB_CONFIG, HEARTBEAT, REREGISTER, FILE_APPLY | wp-0, wp-3 |
| wp-5 | 实例生命周期与监控 | 超时下线、归档、历史清理、实例查询 | TIMEOUT, ARCHIVE, HISTORY_DELETE, INST_LIST, INST_OFFLINE, INST_DETAIL | wp-0, wp-4 |
| wp-6 | 审计与系统配置 | 审计日志、配置查看/修改 | AUDIT, CONFIG_VIEW, CONFIG_HB | wp-0, wp-2 |
| wp-7 | 弹性配额与多节点一致性 | 弹性配额判定、多节点强一致 | ELASTIC_QUOTA, MULTI_NODE | wp-0, wp-4 |
| wp-8 | 统计与仪表盘 | 统计概览、趋势、仪表盘/告警、统计导出 | STAT_OVERVIEW, STAT_TREND, DASHBOARD, STAT_EXPORT | wp-0, wp-5 |
| wp-9 | 客户端 SDK | 嵌入 AS JVM 的 SDK 生命周期 + local/site 文件申请 | 本地校验、注册、心跳、重注册、文件申请（SDK 侧） | wp-0, wp-4 |
| wp-10 | 管理控制台前端 + 集成验收 | 管理页面（含统计报表/仪表盘）+ 端到端集成 | 各 admin 用例前端 + 全链路 | wp-2~wp-9 |

## 10. 交付顺序与里程碑

1. **M0（wp-0）**：共享契约冻结——DDL、错误码、API 骨架、签名 canonical 规范、构建脚手架。所有并行工作包以此为接缝。
2. **M1（wp-1, wp-2, wp-9）**：签发端、服务端启动/认证、SDK 骨架并行。
3. **M2（wp-3, wp-4）**：授权管理与客户端交互（注册/心跳/文件申请）——核心业务链路。
4. **M3（wp-5, wp-6, wp-7, wp-8）**：实例生命周期/监控、审计/配置、弹性配额/多节点、统计与仪表盘。
5. **M4（wp-10）**：管理控制台前端 + 端到端集成验收。