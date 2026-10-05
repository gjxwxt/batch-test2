# 共享契约 · 错误码目录（infra:error-codes）

> 冻结基线（wp-0）。全服务端共用。Java 枚举：`server/src/main/java/com/example/app/exception/ErrorCode.java`。
> 需求来源：`center模式-服务端需求文档(1).md` 6.4 节错误码完整清单。

## 错误码清单

| 错误码 | 含义 | 触发场景 |
|--------|------|----------|
| SUCCESS | 操作成功 | 正常流程 |
| WARNING | 操作成功（有警告） | 弹性配额、授权过期心跳 |
| AUTH_001 | 用户名或密码错误 | 登录 |
| AUTH_002 | 登录已过期 | JWT 过期 |
| USER_001 | 用户不存在 | 查询用户 |
| USER_003 | 旧密码错误 | 修改密码 |
| USER_004 | 新密码复杂度不足 | 修改密码 |
| LICENSE_001 | 授权不存在 | 注册/查询 |
| LICENSE_002 | 授权签名验证失败 | 导入/防篡改 |
| LICENSE_003 | CPU/内存配额不足 | 注册 |
| LICENSE_004 | 授权已过期 | 注册/导入 |
| LICENSE_005 | 授权已存在（重复导入） | 导入 |
| LICENSE_006 | 存在在线实例，无法删除 | 删除授权 |
| INSTANCE_001 | 弹性运行模式提示 | 注册 |
| INSTANCE_002 | 实例不存在 | 心跳 → 触发重注册 |
| INSTANCE_003 | 授权已过期 | 心跳 |
| INSTANCE_004 | 配额超 200% 上限 | 注册 |
| PARAM_001 | 参数缺失或格式错误 | 所有接口 |
| SYS_001 | 服务器内部错误 | 异常兜底 |

## 响应语义

- 成功：`{ "code": "SUCCESS", "data": {...} }`（或 `WARNING` 带 `data.warning`）。
- 失败：统一 `ApiErrorResponse`（`code`/`message`/`details`/`timestamp`），HTTP 状态码由 `GlobalExceptionHandler` 映射。
- 所有 controller 不得自行发明错误码，一律引用 `ErrorCode` 枚举。

## 变更控制

错误码为冻结契约。新增错误码须经 contract_review 评审，不得修改既有 code 语义。