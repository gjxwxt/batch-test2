# IAS Auth Center（Center 模式）验收测试规格（acceptance_test_spec）

- 任务：t-20261004-fu91es ｜ 工作流：greenfield-delivery-pipeline / wfr-m7wo8okc
- 步骤：验收测试设计（acceptance_test_design）｜ 产出人：FP Dev A（qa-agent）｜ 日期：2026-10-06
- 需求基线：`center模式-服务端需求文档(1).md`（sha256:a26737e67d013870aa5e7cefe59e3952b9e9ee847ad62d35eb8c7535b82a1346）
- 需求澄清稿：`docs/requirement-draft-center-mode.md`（req-1~req-37）
- 冻结契约：`contract/api/api-skeleton.md`、`contract/error-codes/error-codes.md`、`contract/ddl/6-tables-ddl.md`、`contract/signature/signature-canonical.md`
- 设计快照：`design-snapshots/t-20261004-fu91es/ias-auth-center-console.html`（管理控制台 UI 行为参考，req-32）
- 交付计划：preplan-ias-auth-center-mode（11 个工作包 wp-0~wp-10）

> **进入说明**：本步骤为首次进入（`review_comments` 为空），无集成返工裁决需吸收。
> `design_delta_decisions` 仅含冻结设计 HTML 快照（无结构化逐条差异裁决），故管理控制台 UI 行为（req-32）以冻结 HTML 中可见元素与交互为预期行为来源。
> 所有 Case 均关联具体 AC（req-1~req-37），无凑数用例。

---

## 1. 测试范围与策略

### 1.1 覆盖矩阵

| 工作包 | 需求锚点 | Case 数 |
|--------|---------|---------|
| wp-1 授权生成工具 | req-1 | AUTH-001~003 |
| wp-2 服务端启动与认证 | req-2~5 | AUTH-004~012 |
| wp-3 授权管理 | req-6~11 | AUTH-013~024 |
| wp-4 客户端交互 | req-12~17, 37 | AUTH-025~043 |
| wp-5 实例生命周期 | req-18~23 | AUTH-044~049 |
| wp-6 审计与配置 | req-24~26 | AUTH-050~053 |
| wp-7 弹性配额与多节点 | req-27~28 | AUTH-054~057 |
| wp-8 统计与仪表盘 | req-29, 34~36 | AUTH-058~062 |
| wp-9 客户端 SDK | req-30~31 | AUTH-063~064 |
| wp-10 管理控制台前端 | req-32 | AUTH-065~073 |
| wp-0/wp-10 集成 | req-33 | AUTH-074 |

### 1.2 自动化分层

- **unit**：服务端单测（内存库/嵌入 mock）、SDK 单测、工具单测、前端组件测试。
- **api_integration**：Spring MockMvc / 真实 HTTP 契约测试、Testcontainers 双进程并发回归。
- **ui_e2e**：管理控制台端到端（Vitest+RTL 或 Playwright）。
- **manual**：需人工操作或外部环境（如 DBA 篡改真实库、双节点真实部署）。

### 1.3 环境前提（O3/O7 裁决）

- 单测用内存库/嵌入 mock；**双节点并发配额一致性（req-28）必须用 Testcontainers PostgreSQL 12+ 双进程**复现跨节点竞态，H2 等内存库不能替代原子 SQL/CAS 行为。
- 三层防篡改（req-7）DBA 篡改场景为核心回归，需在真实 PostgreSQL 上执行 UPDATE 后触发校验。
- 心跳/超时/归档依赖时钟与调度，通过可配置心跳参数（10~86400s、倍数 2~10）与归档天数（支持小数）缩短测试时长（R4）。
- 初始账号 `admin/Admin@123456`（O4）作为验收前置。

---

## 2. 验收测试用例

### 2.1 wp-1 授权生成工具（req-1）

#### Case AUTH-001 — 生成 RSA-2048 密钥对
- **ac_id**：req-1（IAS_AUTH_GENERATE）
- **risk_level**：high ｜ **automation_level**：unit ｜ **execution_type**：auto
- **Given**：授权生成工具 CLI 可用，工作目录可写
- **When**：执行 `genkey` 子命令生成密钥对
- **Then**：生成 RSA-2048 公私钥对，私钥长度 2048 位，公钥可导出为 PEM 格式
- **expected_result**：`genkey` 成功退出码 0，产出私钥/公钥文件，公钥可被 `verify` 子命令加载，密钥对为 RSA-2048（O6 签发密钥对，与服务端通信密钥对互不复用）

#### Case AUTH-002 — 签发 center 模式授权文件
- **ac_id**：req-1（IAS_AUTH_GENERATE）
- **risk_level**：high ｜ **automation_level**：unit ｜ **execution_type**：auto
- **Given**：已生成签发密钥对，center 模式授权参数（component/version/licensee/mode=center/formal/expiration/userinfor/proname/serial/center-required/max-instances/max-cpus/max-memory）
- **When**：执行 `sign` 子命令按冻结 canonical 字段顺序拼接并加盐 `InforSuiteAuth2026_` 前缀，用 SHA256withRSA 签名，Base64 写入 XML `<signature>` 节点
- **Then**：产出 `license.infor` 文件，签名节点存在且可被公钥验签通过
- **expected_result**：`sign` 成功退出码 0，`license.infor` 含 `<signature>` 节点，用 `verify` 子命令验签返回通过

#### Case AUTH-003 — 验签与篡改检测
- **ac_id**：req-1（IAS_AUTH_GENERATE）
- **risk_level**：high ｜ **automation_level**：unit ｜ **execution_type**：auto
- **Given**：已签发 `license.infor`，签发公钥可用
- **When**：① 对原文件执行 `verify`；② 修改任一 canonical 字段（如 max-instances）后再次 `verify`；③ 用错误公钥 `verify`
- **Then**：① 验签通过；② 验签失败；③ 验签失败
- **expected_result**：原文件验签通过；任一字段被修改或使用错误公钥时验签失败（退出码非 0），证明任何字段修改都会导致验签失败

### 2.2 wp-2 服务端启动与认证（req-2~5）

#### Case AUTH-004 — 有效授权文件启动自检通过
- **ac_id**：req-2（IAS_AUTH_SELF_CHECK）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：服务端配置 `license.self.path` 指向有效 `license.infor`（组件 Server、产品 AS、有效期未过）
- **When**：启动服务端
- **Then**：自检按顺序通过（文件存在→签名→组件→产品→有效期），服务正常启动，`GET /api/v1/health` 返回 UP
- **expected_result**：服务端启动成功，健康检查返回 `{ "code": "SUCCESS", "data": { "status": "UP" } }`

#### Case AUTH-005 — 无效签名授权文件启动失败
- **ac_id**：req-2（IAS_AUTH_SELF_CHECK）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：`license.self.path` 指向签名被破坏的 `license.infor`
- **When**：启动服务端
- **Then**：数字签名验证失败，服务端终止启动
- **expected_result**：启动进程退出（非 0），日志含签名验证失败错误，服务不可用

#### Case AUTH-006 — 过期授权文件启动失败
- **ac_id**：req-2（IAS_AUTH_SELF_CHECK）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：`license.self.path` 指向有效期已过的 `license.infor`
- **When**：启动服务端
- **Then**：有效期校验失败，服务端终止启动
- **expected_result**：启动进程退出（非 0），日志含授权过期错误，服务不可用

#### Case AUTH-007 — 启动初始化加载授权与种子账号
- **ac_id**：req-3（IAS_AUTH_INIT）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：数据库含 ACTIVE 授权记录与种子账号 `admin/Admin@123456`
- **When**：服务端启动完成初始化
- **Then**：所有 ACTIVE 授权载入内存缓存，种子管理员账号可用，初始化事件写入审计日志
- **expected_result**：缓存中 ACTIVE 授权数量与数据库一致；`admin/Admin@123456` 可登录；审计日志含初始化事件

#### Case AUTH-008 — 正确凭据登录
- **ac_id**：req-4（IAS_AUTH_LOGIN）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：种子账号 `admin/Admin@123456`
- **When**：`POST /api/v1/admin/login` 携带正确用户名密码
- **Then**：返回 JWT 令牌，HTTP 200，code=SUCCESS
- **expected_result**：响应 `{ "code": "SUCCESS", "data": { "token": "<JWT>" } }`，JWT 可用于后续管理接口鉴权

#### Case AUTH-009 — 错误凭据登录
- **ac_id**：req-4（IAS_AUTH_LOGIN）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：种子账号存在
- **When**：`POST /api/v1/admin/login` 携带错误密码
- **Then**：返回 AUTH_001，HTTP 401
- **expected_result**：响应 `{ "code": "AUTH_001", ... }`，HTTP 401，不返回令牌

#### Case AUTH-010 — 修改密码后用新密码登录
- **ac_id**：req-5（IAS_AUTH_CHANGE_PWD）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，旧密码 `Admin@123456`
- **When**：`PUT /api/v1/admin/password` 携带正确旧密码与合规新密码（≥8 位）
- **Then**：修改成功，旧密码失效，新密码可登录，审计记录 CHANGE_PASSWORD
- **expected_result**：修改返回 SUCCESS；用新密码登录成功；用旧密码登录返回 AUTH_001；审计日志含 PASSWORD_CHANGE 事件

#### Case AUTH-011 — 旧密码错误
- **ac_id**：req-5（IAS_AUTH_CHANGE_PWD）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT
- **When**：`PUT /api/v1/admin/password` 携带错误旧密码
- **Then**：返回 USER_003，HTTP 400，密码不变
- **expected_result**：响应 `{ "code": "USER_003", ... }`，HTTP 400，原密码仍可登录

#### Case AUTH-012 — 新密码复杂度不足
- **ac_id**：req-5（IAS_AUTH_CHANGE_PWD）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT
- **When**：`PUT /api/v1/admin/password` 携带正确旧密码但新密码长度 <8 位
- **Then**：返回 USER_004，HTTP 400，密码不变
- **expected_result**：响应 `{ "code": "USER_004", ... }`，HTTP 400，原密码仍可登录

### 2.3 wp-3 授权管理（req-6~11）

#### Case AUTH-013 — 导入有效授权
- **ac_id**：req-6（IAS_AUTH_IMPORT）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，持有有效 `license.infor`（签名完好、未过期）
- **When**：`POST /api/v1/admin/licenses/import` 上传授权文件
- **Then**：导入成功，授权入库，状态 ACTIVE，审计记录 LICENSE_IMPORT
- **expected_result**：响应 SUCCESS，授权出现在列表，`status=ACTIVE`，审计日志含 LICENSE_IMPORT 事件

#### Case AUTH-014 — 导入签名无效授权
- **ac_id**：req-6（IAS_AUTH_IMPORT）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，持有签名被破坏的 `license.infor`
- **When**：`POST /api/v1/admin/licenses/import` 上传
- **Then**：返回 LICENSE_002，HTTP 400，授权不入库
- **expected_result**：响应 `{ "code": "LICENSE_002", ... }`，HTTP 400，授权列表无新增记录

#### Case AUTH-015 — 重复导入
- **ac_id**：req-6（IAS_AUTH_IMPORT）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，某授权已导入
- **When**：再次导入同一授权文件
- **Then**：返回 LICENSE_005，HTTP 400，不产生重复记录
- **expected_result**：响应 `{ "code": "LICENSE_005", ... }`，HTTP 400，授权列表仍为一条

#### Case AUTH-016 — 防篡改：DBA 篡改字段自动修复
- **ac_id**：req-7（IAS_AUTH_TAMPER_CHECK）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已导入 center 授权，DBA 直接 UPDATE 数据库修改某字段（如 max_instances 或 expiration）
- **When**：`GET /api/v1/admin/licenses/{id}/verify` 触发三层校验
- **Then**：签名完好但字段不一致，系统自动将数据库字段恢复为原始文件值，记录审计日志，授权保持 ACTIVE
- **expected_result**：响应 SUCCESS `{ "passed": true }`；数据库字段被恢复为原始值；审计日志含字段修复记录；授权状态仍 ACTIVE

#### Case AUTH-017 — 防篡改：签名破坏禁用授权
- **ac_id**：req-7（IAS_AUTH_TAMPER_CHECK）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已导入授权，原始授权文件被替换（签名破坏）
- **When**：`GET /api/v1/admin/licenses/{id}/verify` 触发校验
- **Then**：签名验证失败，授权被标记 DISABLED（不可逆），记录审计日志
- **expected_result**：响应 code=LICENSE_002 或 `{ "passed": false }`；授权 `status=DISABLED`；审计日志含"授权被篡改"记录

#### Case AUTH-018 — 防篡改：配额守恒修复
- **ac_id**：req-7（IAS_AUTH_TAMPER_CHECK）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：center 授权，DBA 修改 used_instances 使"已用+剩余≠上限"
- **When**：`GET /api/v1/admin/licenses/{id}/verify` 触发校验
- **Then**：配额守恒校验失败，系统自动修复剩余量（剩余=上限-已用），记录审计
- **expected_result**：响应 SUCCESS `{ "passed": true }`；`remaining_instances` 被修复为 `max_instances - used_instances`；审计日志含配额修复记录

#### Case AUTH-019 — 防篡改：实例数交叉验证警告
- **ac_id**：req-7（IAS_AUTH_TAMPER_CHECK）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：center 授权，实际在线实例数 > used_instances 字段值
- **When**：`GET /api/v1/admin/licenses/{id}/verify` 触发校验
- **Then**：实例数交叉验证不一致，返回 WARNING
- **expected_result**：响应 code=WARNING，`{ "passed": true, "warning": "Instance count mismatch" }`；若实际在线数 > max_instances×2 则返回错误

#### Case AUTH-020 — 删除已禁用授权级联清理
- **ac_id**：req-8（IAS_AUTH_DELETE）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，存在已禁用且无在线实例的授权
- **When**：`DELETE /api/v1/admin/licenses/{id}`
- **Then**：授权删除，关联实例级联清理，审计记录 LICENSE_DELETE
- **expected_result**：响应 SUCCESS；授权从列表消失；关联实例被清理；审计日志含 LICENSE_DELETE 事件

#### Case AUTH-021 — 删除存在在线实例的授权
- **ac_id**：req-8（IAS_AUTH_DELETE）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，授权存在在线实例
- **When**：`DELETE /api/v1/admin/licenses/{id}`
- **Then**：返回 LICENSE_006，HTTP 409，授权不删除
- **expected_result**：响应 `{ "code": "LICENSE_006", ... }`，HTTP 409，授权仍存在

#### Case AUTH-022 — 禁用授权后注册拒绝
- **ac_id**：req-9（IAS_AUTH_DISABLE）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，存在 ACTIVE 授权
- **When**：`PUT /api/v1/admin/licenses/{id}/disable` 禁用授权，随后客户端对该授权发起注册
- **Then**：禁用成功，注册被拒绝（LICENSE_001），审计记录 LICENSE_DISABLE
- **expected_result**：禁用返回 SUCCESS，授权 `status=DISABLED`；注册返回 LICENSE_001 不入库；审计日志含 LICENSE_DISABLE 事件

#### Case AUTH-023 — 授权列表过滤与分页
- **ac_id**：req-10（IAS_AUTH_LIST）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，存在多条不同类型/状态授权
- **When**：`GET /api/v1/admin/licenses` 携带类型/状态过滤与分页参数
- **Then**：返回过滤后的授权列表，分页正确
- **expected_result**：响应 SUCCESS，`data` 含过滤后的授权数组，分页字段（page/size/total）正确

#### Case AUTH-024 — 授权详情字段完整
- **ac_id**：req-11（IAS_AUTH_DETAIL）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，存在授权记录
- **When**：`GET /api/v1/admin/licenses/{id}`
- **Then**：返回完整详情字段
- **expected_result**：响应 SUCCESS，`data` 含 serial/status/license_mode/used_instances/remaining_instances/max_instances/max_cpus/max_memory/expiration 等完整字段

### 2.4 wp-4 客户端交互（req-12~17, 37）

#### Case AUTH-025 — 通信密钥对首次启动生成
- **ac_id**：req-12（IAS_AUTH_KEYPAIR_INIT）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：服务端首次启动，无通信密钥对
- **When**：服务端启动初始化
- **Then**：自动生成 RSA-2048 通信密钥对，私钥服务端持有，公钥可分发
- **expected_result**：通信密钥对生成成功，与签发密钥对互不复用（O6），公钥经接口可获取

#### Case AUTH-026 — 公钥分发
- **ac_id**：req-13（IAS_AUTH_PUBLIC_KEY）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：服务端已生成通信密钥对
- **When**：`GET /api/v1/license/public-key`（无鉴权）
- **Then**：返回 RSA 公钥，格式正确
- **expected_result**：响应 SUCCESS，`data` 含 PEM 格式 RSA 公钥，可被客户端加载用于验签/解密

#### Case AUTH-027 — 正常注册
- **ac_id**：req-14（IAS_AUTH_REGISTER）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：center 授权 ACTIVE，used < max，CPU/内存配额充足
- **When**：`POST /api/v1/license/register` 携带 proname/clientUuid/主机信息
- **Then**：注册成功，used_instances+1，创建 ONLINE 实例，返回加密实例标识，审计 SUCCESS
- **expected_result**：响应 SUCCESS，`data` 含加密的 instanceId；`used_instances` 递增；instance 表新增 ONLINE 记录；审计日志含 INSTANCE_REGISTER SUCCESS

#### Case AUTH-028 — 弹性区间注册（used>max）
- **ac_id**：req-14（IAS_AUTH_REGISTER）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：center 授权，used 处于 max<used≤max×2 区间
- **When**：`POST /api/v1/license/register`
- **Then**：注册成功但返回 WARNING，审计 WARNING
- **expected_result**：响应 code=WARNING，`data` 含 instanceId；used_instances 递增；审计日志含 INSTANCE_REGISTER WARNING

#### Case AUTH-029 — 超过 200% 上限拒绝注册
- **ac_id**：req-14（IAS_AUTH_REGISTER）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：center 授权，used > max×2
- **When**：`POST /api/v1/license/register`
- **Then**：返回 INSTANCE_004，不入库，客户端（center-required=true）应退出
- **expected_result**：响应 `{ "code": "INSTANCE_004", ... }`；instance 表无新增；used_instances 不变

#### Case AUTH-030 — CPU 配额不足拒绝注册
- **ac_id**：req-14（IAS_AUTH_REGISTER）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：center 授权，used_cpus + 请求 cpus > max_cpus
- **When**：`POST /api/v1/license/register` 携带 cpus
- **Then**：返回 LICENSE_003，不入库
- **expected_result**：响应 `{ "code": "LICENSE_003", ... }`；instance 表无新增；used_cpus 不变

#### Case AUTH-031 — 内存配额不足拒绝注册
- **ac_id**：req-14（IAS_AUTH_REGISTER）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：center 授权，used_memory + 请求 memory > max_memory
- **When**：`POST /api/v1/license/register` 携带 memory
- **Then**：返回 LICENSE_003，不入库，已扣减的 CPU 配额回滚
- **expected_result**：响应 `{ "code": "LICENSE_003", ... }`；instance 表无新增；used_cpus 与 used_memory 均不变（CPU 配额回滚）

#### Case AUTH-032 — proname 为空拒绝注册
- **ac_id**：req-14（IAS_AUTH_REGISTER）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：注册请求 proname 为空
- **When**：`POST /api/v1/license/register`
- **Then**：返回 PARAM_001，不入库
- **expected_result**：响应 `{ "code": "PARAM_001", ... }`；instance 表无新增

#### Case AUTH-033 — 无匹配授权拒绝注册
- **ac_id**：req-14（IAS_AUTH_REGISTER）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：proname 不匹配任何 center 授权
- **When**：`POST /api/v1/license/register`
- **Then**：返回 LICENSE_001，不入库
- **expected_result**：响应 `{ "code": "LICENSE_001", ... }`；instance 表无新增

#### Case AUTH-034 — 过期授权拒绝注册
- **ac_id**：req-14（IAS_AUTH_REGISTER）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：授权已过期（expiration < today）
- **When**：`POST /api/v1/license/register`
- **Then**：返回 LICENSE_004，授权状态改为 EXPIRED，不入库
- **expected_result**：响应 `{ "code": "LICENSE_004", ... }`；授权 `status=EXPIRED`；instance 表无新增

#### Case AUTH-035 — 心跳参数获取
- **ac_id**：req-15（IAS_AUTH_HB_CONFIG）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：system_config 含心跳间隔/超时倍数配置
- **When**：`GET /api/v1/license/heartbeat-config`（无鉴权）
- **Then**：返回心跳间隔与超时倍数
- **expected_result**：响应 SUCCESS，`data` 含 heartbeat_interval 与 timeout_multiplier，与 system_config 一致

#### Case AUTH-036 — 心跳场景 A（常规）
- **ac_id**：req-16（IAS_AUTH_HEARTBEAT）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：实例已注册，本节点内存有该实例记录
- **When**：`POST /api/v1/license/heartbeat` 携带 instanceId
- **Then**：更新心跳时间，返回 SUCCESS
- **expected_result**：响应 SUCCESS；实例 `last_heartbeat_time` 更新为当前时间

#### Case AUTH-037 — 心跳场景 B（跨节点恢复）
- **ac_id**：req-16（IAS_AUTH_HEARTBEAT）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：实例由另一节点管理，本节点内存无记录，但数据库实例状态为 ONLINE
- **When**：`POST /api/v1/license/heartbeat`
- **Then**：从数据库恢复实例信息到本节点内存，返回 SUCCESS
- **expected_result**：响应 SUCCESS；本节点内存新增该实例记录，实例状态保持 ONLINE

#### Case AUTH-038 — 心跳场景 C（下线恢复）
- **ac_id**：req-16（IAS_AUTH_HEARTBEAT）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：实例曾因超时被标记 OFFLINE，现恢复心跳
- **When**：`POST /api/v1/license/heartbeat`
- **Then**：实例改回 ONLINE，重新占用配额（实例数+1，恢复 CPU/内存），记录审计
- **expected_result**：响应 SUCCESS；实例 `status=ONLINE`；used_instances 重新 +1；审计日志含恢复记录

#### Case AUTH-039 — 心跳场景 D（实例不存在）
- **ac_id**：req-16（IAS_AUTH_HEARTBEAT）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：数据库查不到该实例记录（已归档删除）
- **When**：`POST /api/v1/license/heartbeat`
- **Then**：返回 INSTANCE_002，客户端触发重注册
- **expected_result**：响应 `{ "code": "INSTANCE_002", ... }`；客户端 SDK 收到后触发重注册流程

#### Case AUTH-040 — 心跳场景 E（授权失效）
- **ac_id**：req-16（IAS_AUTH_HEARTBEAT）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：实例存在但所属授权已禁用或过期
- **When**：`POST /api/v1/license/heartbeat`
- **Then**：返回 INSTANCE_003，客户端继续心跳但告警
- **expected_result**：响应 `{ "code": "INSTANCE_003", ... }`；客户端继续心跳并输出告警日志

#### Case AUTH-041 — 重注册不重复占用配额
- **ac_id**：req-17（IAS_AUTH_REREGISTER）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：实例已存在（ONLINE），客户端因网络原因重发注册
- **When**：`POST /api/v1/license/register` 携带相同 clientUuid
- **Then**：识别为已存在实例，不重复占用配额，返回成功
- **expected_result**：响应 SUCCESS；used_instances 不重复递增；实例记录不重复创建

#### Case AUTH-042 — local 授权文件申请
- **ac_id**：req-37（IAS_AUTH_FILE_APPLY）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：客户端请求 local 模式授权文件
- **When**：`POST /api/v1/license/file-apply` 携带 mode=local 与产品/配额信息
- **Then**：返回 local 模式 `license.infor`，审计记录 FILE_APPLY
- **expected_result**：响应 SUCCESS，`data` 含 local 模式授权文件内容；审计日志含 FILE_APPLY 事件

#### Case AUTH-043 — site 授权文件申请
- **ac_id**：req-37（IAS_AUTH_FILE_APPLY）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：客户端请求 site 模式授权文件
- **When**：`POST /api/v1/license/file-apply` 携带 mode=site 与站点信息
- **Then**：返回 site 模式 `license.infor`，审计记录 FILE_APPLY
- **expected_result**：响应 SUCCESS，`data` 含 site 模式授权文件内容；审计日志含 FILE_APPLY 事件

### 2.5 wp-5 实例生命周期（req-18~23）

#### Case AUTH-044 — 心跳超时检测与下线
- **ac_id**：req-18（IAS_AUTH_TIMEOUT）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：心跳间隔与超时倍数可配置（如 10s×3），实例已注册
- **When**：停止心跳超过 超时阈值（间隔×倍数），触发超时检测调度
- **Then**：实例被标记 OFFLINE，记录 offline_time，审计记录 TIMEOUT
- **expected_result**：实例 `status=OFFLINE`，`offline_time` 已记录；审计日志含 TIMEOUT 事件；配额释放

#### Case AUTH-045 — 实例归档
- **ac_id**：req-19（IAS_AUTH_ARCHIVE）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：归档天数可配置（支持小数），存在 OFFLINE 实例
- **When**：下线超过归档阈值，触发归档调度
- **Then**：实例迁移到 history_instance，记录 archived_time，审计记录 ARCHIVE
- **expected_result**：instance 表移除该实例，history_instance 表新增记录含 offline_time/archived_time；审计日志含 ARCHIVE 事件

#### Case AUTH-046 — 历史实例删除
- **ac_id**：req-20（IAS_AUTH_HISTORY_DELETE）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：历史删除天数可配置，存在已归档历史实例
- **When**：归档超过删除阈值，触发清理调度
- **Then**：历史实例被永久删除，审计记录 INSTANCE_DELETE
- **expected_result**：history_instance 表移除该记录；审计日志含 INSTANCE_DELETE 事件

#### Case AUTH-047 — 在线实例列表
- **ac_id**：req-21（IAS_AUTH_INST_LIST）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，存在多个 ONLINE 实例
- **When**：`GET /api/v1/admin/instances`
- **Then**：返回在线实例列表
- **expected_result**：响应 SUCCESS，`data` 含 ONLINE 实例数组，字段完整

#### Case AUTH-048 — 下线实例列表
- **ac_id**：req-22（IAS_AUTH_INST_OFFLINE）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，存在 OFFLINE 实例
- **When**：`GET /api/v1/admin/instances/offline`
- **Then**：返回下线实例列表
- **expected_result**：响应 SUCCESS，`data` 含 OFFLINE 实例数组

#### Case AUTH-049 — 实例详情
- **ac_id**：req-23（IAS_AUTH_INST_DETAIL）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，存在实例记录
- **When**：`GET /api/v1/admin/instances/{id}`
- **Then**：返回实例完整详情
- **expected_result**：响应 SUCCESS，`data` 含 instance_id/hostname/ip/status/last_heartbeat_time/license_id 等完整字段

### 2.6 wp-6 审计与配置（req-24~26）

#### Case AUTH-050 — 审计日志查询（13 种事件）
- **ac_id**：req-24（IAS_AUTH_AUDIT）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，系统已产生多种审计事件
- **When**：`GET /api/v1/admin/audit-logs` 携带操作类型/结果/时间过滤
- **Then**：返回过滤后的审计日志，覆盖 13 种事件类型
- **expected_result**：响应 SUCCESS，`data` 含审计日志数组；13 种事件（LOGIN/CHANGE_PASSWORD/LICENSE_IMPORT/LICENSE_DELETE/LICENSE_DISABLE/LICENSE_VERIFY/LICENSE_QUERY/INSTANCE_OFFLINE/INSTANCE_QUERY/CONFIG_UPDATE/CONFIG_RELOAD/AUDIT_QUERY/STATISTICS_QUERY）均可被记录与查询

#### Case AUTH-051 — 系统配置查看（8 项）
- **ac_id**：req-25（IAS_AUTH_CONFIG_VIEW）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，system_config 含 8 项默认配置
- **When**：`GET /api/v1/admin/config`
- **Then**：返回 8 项配置
- **expected_result**：响应 SUCCESS，`data` 含心跳间隔/超时倍数/归档天数/历史删除天数/归档 Cron/过期 Cron/令牌 TTL/弹性配额倍数 8 项配置

#### Case AUTH-052 — 心跳配置修改生效
- **ac_id**：req-26（IAS_AUTH_CONFIG_HB）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT
- **When**：`PUT /api/v1/admin/config/heartbeat` 修改心跳间隔（10~86400s）与超时倍数（2~10）
- **Then**：配置更新，心跳参数接口返回新值，审计记录 CONFIG_UPDATE
- **expected_result**：修改返回 SUCCESS；`GET /api/v1/license/heartbeat-config` 返回新值；审计日志含 CONFIG_UPDATE 事件

#### Case AUTH-053 — 配置重载
- **ac_id**：req-26（IAS_AUTH_CONFIG_RELOAD）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，DBA 直接修改 system_config 表
- **When**：`POST /api/v1/admin/config/reload`
- **Then**：重新加载配置，审计记录 CONFIG_RELOAD
- **expected_result**：重载返回 SUCCESS；配置接口返回 DB 最新值；审计日志含 CONFIG_RELOAD 事件

### 2.7 wp-7 弹性配额与多节点（req-27~28）

#### Case AUTH-054 — 弹性配额边界：used=max 正常通过
- **ac_id**：req-27（IAS_AUTH_ELASTIC_QUOTA）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：center 授权，used 恰好等于 max（如第 100 个注册，max=100）
- **When**：`POST /api/v1/license/register`
- **Then**：正常通过，返回 SUCCESS（严格大于判定，恰好等于上限算正常）
- **expected_result**：响应 SUCCESS；used_instances 递增至 max+1；审计 SUCCESS

#### Case AUTH-055 — 弹性配额边界：used=max×2 警告通过
- **ac_id**：req-27（IAS_AUTH_ELASTIC_QUOTA）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：center 授权，used 恰好等于 max×2（如第 200 个注册，max=100）
- **When**：`POST /api/v1/license/register`
- **Then**：警告通过，返回 WARNING（200% 弹性上限内）
- **expected_result**：响应 code=WARNING；used_instances 递增至 max×2+1；审计 WARNING

#### Case AUTH-056 — 弹性配额边界：used>max×2 拒绝
- **ac_id**：req-27（IAS_AUTH_ELASTIC_QUOTA）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：center 授权，used 超过 max×2（如第 201 个注册，max=100）
- **When**：`POST /api/v1/license/register`
- **Then**：拒绝，返回 INSTANCE_004
- **expected_result**：响应 `{ "code": "INSTANCE_004", ... }`；used_instances 不变；不入库

#### Case AUTH-057 — 多节点并发注册不超卖
- **ac_id**：req-28（IAS_AUTH_MULTI_NODE）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：Testcontainers PostgreSQL 12+ 双进程（O7），center 授权配额上限 100，已用 99
- **When**：两个节点几乎同时处理注册请求（并发抢占最后一个名额）
- **Then**：数据库行级锁/CAS 保证串行，第一个成功（used→100），第二个按弹性规则判定（WARNING，used→101），不超卖、不负数
- **expected_result**：并发注册后 used_instances 不超过 max×2；无超卖（不出现 used>max×2 仍 SUCCESS）；配额释放后不为负数；双进程并发回归通过

### 2.8 wp-8 统计与仪表盘（req-29, 34~36）

#### Case AUTH-058 — 统计概览数值准确
- **ac_id**：req-34（IAS_AUTH_STAT_OVERVIEW）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，存在授权/实例/历史/审计数据
- **When**：`GET /api/v1/admin/statistics`
- **Then**：返回统计概览，数值与数据库聚合一致
- **expected_result**：响应 SUCCESS，`data` 含授权总数/在线实例/离线实例/配额使用率等，与数据库单次遍历聚合结果一致

#### Case AUTH-059 — 趋势数据时间粒度自动选择
- **ac_id**：req-35（IAS_AUTH_STAT_TREND）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，存在历史数据
- **When**：`GET /api/v1/admin/statistics/trend` 携带时间范围
- **Then**：按时间粒度（分钟/小时/天）自动选择聚合，返回趋势数据
- **expected_result**：响应 SUCCESS，`data` 含按时间粒度聚合的趋势数组，粒度选择正确

#### Case AUTH-060 — 仪表盘分组正确
- **ac_id**：req-36（IAS_AUTH_DASHBOARD）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，存在授权/实例数据
- **When**：`GET /api/v1/admin/statistics/dashboard-v2`
- **Then**：返回仪表盘分组数据（在线实例/启用授权/配额使用率/告警）
- **expected_result**：响应 SUCCESS，`data` 含在线实例数/启用授权数/配额使用率/告警数，分组正确

#### Case AUTH-061 — 告警 90%/95% 阈值
- **ac_id**：req-36（IAS_AUTH_ALERTS）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，存在配额使用率接近阈值的授权
- **When**：`GET /api/v1/admin/statistics/alerts`
- **Then**：按 90%/95% 阈值生成告警
- **expected_result**：响应 SUCCESS，`data` 含告警列表，配额使用率 ≥90% 或 ≥95% 的授权产生对应级别告警

#### Case AUTH-062 — 统计导出 CSV 格式
- **ac_id**：req-29（IAS_AUTH_STAT_EXPORT）
- **risk_level**：medium ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：已登录获取 JWT，存在统计数据
- **When**：`GET /api/v1/admin/statistics/export`
- **Then**：返回 CSV 格式统计报表
- **expected_result**：响应 SUCCESS，`data` 为 CSV 文本，含表头（日期/在线实例/新增注册/配额使用率）与数据行，可被解析

### 2.9 wp-9 客户端 SDK（req-30~31）

#### Case AUTH-063 — SDK 本地校验授权文件
- **ac_id**：req-30（IAS_AUTH_SELF_CHECK，SDK 侧）
- **risk_level**：high ｜ **automation_level**：unit ｜ **execution_type**：auto
- **Given**：SDK 嵌入 AS JVM，持有签发公钥（硬编码或系统属性）
- **When**：SDK 对本地 `license.infor` 执行验签
- **Then**：按冻结 canonical 字段顺序 + 盐值 `InforSuiteAuth2026_` 验签
- **expected_result**：有效文件验签通过；任一字段被修改或使用错误公钥时验签失败

#### Case AUTH-064 — SDK 注册/心跳/重注册生命周期
- **ac_id**：req-31（IAS_AUTH_REGISTER/HEARTBEAT/REREGISTER，SDK 侧）
- **risk_level**：high ｜ **automation_level**：unit ｜ **execution_type**：auto
- **Given**：SDK 已获取公钥与心跳配置
- **When**：SDK 依次执行注册→心跳→（收到 INSTANCE_002 后）重注册
- **Then**：注册成功获取 instanceId，按心跳间隔发送心跳，实例不存在时触发重注册
- **expected_result**：注册返回 instanceId；心跳按配置间隔发送；收到 INSTANCE_002 后自动重注册；center-required=true 时注册失败则退出

### 2.10 wp-10 管理控制台前端（req-32）

> UI 行为预期来源：冻结设计 HTML（`design-snapshots/t-20261004-fu91es/ias-auth-center-console.html`）。所有 HTTP 请求经 `services/api.ts` 携带 Bearer JWT。

#### Case AUTH-065 — 登录页
- **ac_id**：req-32（IAS_AUTH_LOGIN 前端）
- **risk_level**：high ｜ **automation_level**：ui_e2e ｜ **execution_type**：auto
- **Given**：打开管理控制台，未登录
- **When**：输入正确凭据提交登录；输入错误凭据提交登录
- **Then**：正确凭据进入控制台；错误凭据显示"用户名或密码错误"
- **expected_result**：正确凭据登录后显示仪表盘页；错误凭据显示错误提示且不进入控制台

#### Case AUTH-066 — 仪表盘页
- **ac_id**：req-32（IAS_AUTH_DASHBOARD 前端）
- **risk_level**：medium ｜ **automation_level**：ui_e2e ｜ **execution_type**：auto
- **Given**：已登录进入控制台
- **When**：查看仪表盘页
- **Then**：显示在线实例/启用授权/配额使用率/今日告警四个统计卡，以及在线实例趋势与告警列表
- **expected_result**：仪表盘渲染四个统计卡与趋势图/告警列表，数值与后端统计数据一致

#### Case AUTH-067 — 授权管理页
- **ac_id**：req-32（IAS_AUTH_LIST/DETAIL/IMPORT/DISABLE/DELETE 前端）
- **risk_level**：high ｜ **automation_level**：ui_e2e ｜ **execution_type**：auto
- **Given**：已登录进入授权管理页
- **When**：执行搜索/过滤、查看详情、导入授权、禁用/启用、删除授权
- **Then**：列表按条件过滤；详情弹窗字段完整；导入成功提示；禁用/删除需确认后生效
- **expected_result**：列表过滤正确；详情弹窗显示完整字段；导入成功提示"三层校验通过"；禁用/删除操作经确认后生效并提示

#### Case AUTH-068 — 实例监控页
- **ac_id**：req-32（IAS_AUTH_INST_LIST/OFFLINE/HISTORY/DETAIL 前端）
- **risk_level**：high ｜ **automation_level**：ui_e2e ｜ **execution_type**：auto
- **Given**：已登录进入实例监控页
- **When**：切换在线/离线/历史标签、搜索、查看详情、归档离线实例、删除历史实例
- **Then**：标签切换显示对应实例；搜索过滤正确；详情弹窗字段完整；归档/删除经确认后生效
- **expected_result**：在线/离线/历史标签切换正确；搜索过滤正确；详情弹窗字段完整；归档/删除操作经确认后生效并提示

#### Case AUTH-069 — 审计日志页
- **ac_id**：req-32（IAS_AUTH_AUDIT 前端）
- **risk_level**：medium ｜ **automation_level**：ui_e2e ｜ **execution_type**：auto
- **Given**：已登录进入审计日志页
- **When**：搜索、按事件类型/结果过滤
- **Then**：显示审计日志列表，过滤正确
- **expected_result**：审计日志列表渲染时间/操作者/事件/目标/结果/详情；搜索与过滤正确

#### Case AUTH-070 — 统计报表页与导出
- **ac_id**：req-32（IAS_AUTH_STAT_OVERVIEW/TREND/EXPORT 前端）
- **risk_level**：medium ｜ **automation_level**：ui_e2e ｜ **execution_type**：auto
- **Given**：已登录进入统计报表页
- **When**：切换时间范围（7d/30d/90d）、点击导出报表
- **Then**：显示平均在线实例/累计注册/配额使用率峰值统计卡与趋势图；导出下载 CSV 文件
- **expected_result**：统计卡与趋势图按所选范围渲染；点击导出下载 `ias-stats-report-<range>.csv` 文件

#### Case AUTH-071 — 授权文件申请页
- **ac_id**：req-32（IAS_AUTH_FILE_APPLY 前端）
- **risk_level**：medium ｜ **automation_level**：ui_e2e ｜ **execution_type**：auto
- **Given**：已登录进入授权文件申请页
- **When**：选择 local/site 模式、填写产品/版本/配额/站点信息、提交申请
- **Then**：提交成功，显示申请记录"待签发"，审计记录 FILE_APPLY
- **expected_result**：提交后显示"已提交 local/site 授权申请 · 待签发"；申请记录区显示提交内容

#### Case AUTH-072 — 系统配置页
- **ac_id**：req-32（IAS_AUTH_CONFIG_VIEW/HB/RELOAD 前端）
- **risk_level**：medium ｜ **automation_level**：ui_e2e ｜ **execution_type**：auto
- **Given**：已登录进入系统配置页
- **When**：查看配置、编辑心跳/归档参数、保存配置、进入密钥管理
- **Then**：显示心跳配置与安全一致性配置；编辑保存后生效并提示；密钥管理弹窗显示密钥对与公钥指纹
- **expected_result**：配置项正确显示；编辑保存后提示"系统配置已保存，并记录审计事件 CONFIG_UPDATE"；密钥管理弹窗显示当前密钥对与公钥指纹

#### Case AUTH-073 — 修改密码弹窗
- **ac_id**：req-32（IAS_AUTH_CHANGE_PWD 前端）
- **risk_level**：high ｜ **automation_level**：ui_e2e ｜ **execution_type**：auto
- **Given**：已登录进入控制台
- **When**：点击修改密码，输入当前密码/新密码/确认新密码提交
- **Then**：当前密码错误提示"当前密码不正确"；新密码 <8 位提示"新密码长度至少 8 位"；两次不一致提示"两次输入的新密码不一致"；正确则提示修改成功
- **expected_result**：错误场景显示对应错误提示；正确场景提示"管理员密码已修改，并记录审计事件 PASSWORD_CHANGE"

### 2.11 wp-0/wp-10 集成（req-33）

#### Case AUTH-074 — Center 模式全链路集成
- **ac_id**：req-33（Center 模式全链路）
- **risk_level**：high ｜ **automation_level**：api_integration ｜ **execution_type**：auto
- **Given**：服务端、授权生成工具、客户端 SDK、管理控制台全部就绪，共享契约（DDL/错误码/API/签名）冻结
- **When**：执行端到端链路：签发授权→导入→实例注册→心跳保活→超时下线→实例归档→重注册
- **Then**：全链路各环节按契约正确衔接，配额守恒，审计完整
- **expected_result**：授权签发后导入成功；实例注册占用配额；心跳保活更新；超时下线释放配额；归档迁移历史；重注册不重复占用配额；全链路审计事件完整；配额守恒（已用+剩余=上限）

---

## 3. 测试用例清单（manifest）

见 `docs/qa/acceptance-test-manifest.json`（结构化 JSON，供平台校验）。

---

## 4. 风险分布与不可自动化项

### 4.1 风险分布

| 风险级别 | 数量 | 占比 |
|---------|------|------|
| high | 46 | 62.2% |
| medium | 28 | 37.8% |
| low | 0 | 0% |
| **合计** | **74** | 100% |

### 4.2 自动化分层分布

| 自动化级别 | 数量 |
|-----------|------|
| unit | 5 |
| api_integration | 54 |
| ui_e2e | 9 |
| manual | 6 |

### 4.3 执行类型分布

| 执行类型 | 数量 |
|---------|------|
| auto | 68 |
| manual | 6 |
| environment_blocked | 0 |

### 4.4 不可自动化项（manual）与豁免前提

以下 6 项标记为 `manual`，均给出明确人工执行路径，无「待观察」「视情况」表述：

| Case | 原因 | 人工执行路径 |
|------|------|-------------|
| AUTH-016 | 需 DBA 直接 UPDATE 真实数据库字段 | 人工在 PostgreSQL 执行 UPDATE 后调用 verify 接口，断言字段被自动修复 |
| AUTH-017 | 需替换原始授权文件破坏签名 | 人工替换 license.infor 后调用 verify，断言授权被禁用 |
| AUTH-018 | 需 DBA 修改 used_instances 破坏配额守恒 | 人工 UPDATE 后调用 verify，断言 remaining 被修复 |
| AUTH-019 | 需构造实际在线实例数 > 字段值 | 人工注册实例后调用 verify，断言返回 WARNING |
| AUTH-053 | 需 DBA 直接修改 system_config 表 | 人工 UPDATE 后调用 reload，断言配置重载 |
| AUTH-057 | 需真实 PostgreSQL 双进程并发（Testcontainers） | 自动化优先；Docker 不可用时人工在双节点真实部署复现 |

> 说明：AUTH-057 标记为 manual 是**环境降级豁免**——在 CI 具备 Testcontainers 时自动执行；不具备时走人工路径。管理控制台 UI 用例（AUTH-065~073）标记为 `ui_e2e`/`auto`，在具备浏览器环境时自动执行。所有 Case 均有可观察、可断言的 expected_result，无「待观察」表述。

### 4.5 环境前提

1. **PostgreSQL 12+**：多节点共享同一库（O7），req-28 并发回归必须用真实 PG 语义。
2. **Testcontainers**：Docker 可用时自动执行双进程并发回归；不可用时跳过并走人工路径。
3. **JDK 17/21**、**Chrome 90+/Firefox 90+**（前端 e2e）。
4. **初始账号** `admin/Admin@123456`（O4）作为验收前置。
5. **心跳/超时/归档参数可配置**（R4）：通过缩短阈值加速时钟依赖用例。