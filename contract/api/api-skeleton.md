# API 骨架契约 (API Skeleton Contract)

> 共享契约条目：`infra:api-skeleton`
> 承载基线 commit：`448ca97f482f6593bac1321a3f7827027c0921a4`
> 分支：`feature/t-20261004-fu91es`

本文档冻结 IAS 授权中心（IAS Auth Center）对外暴露的全部 RESTful API 路径、HTTP 方法与鉴权要求。下游工作包（wp-1..wp-10）必须严格遵循本契约实现，不得擅自增删或改写路径。

## 1. 基础约定

- **根路径**：业务 API 统一使用 `/api/v1` 前缀；健康检查使用 `/api/health`。
- **传输编码**：请求与响应体统一为 UTF-8 JSON（`Content-Type: application/json`）。
- **时间格式**：ISO 8601 字符串，如 `2026-09-09T14:30:00Z`。
- **鉴权前缀**：
  - `/api/v1/admin/**` —— 需 JWT 鉴权（除登录外）。
  - `/api/v1/license/**` —— 无鉴权（客户端注册 / 心跳 / 公钥）。
  - `/api/v1/health` —— 无鉴权。

## 2. 端点清单

### 2.1 管理端（`/api/v1/admin/**`，JWT）

| # | 方法 | 路径 | 用例 | 鉴权 |
|---|------|------|------|------|
| 1 | POST | `/api/v1/admin/login` | IAS_AUTH_LOGIN | 无 |
| 2 | PUT | `/api/v1/admin/password` | IAS_AUTH_CHANGE_PWD | JWT |
| 3 | POST | `/api/v1/admin/licenses/import` | IAS_AUTH_IMPORT | JWT |
| 4 | GET | `/api/v1/admin/licenses` | IAS_AUTH_LIST | JWT |
| 5 | GET | `/api/v1/admin/licenses/{id}` | IAS_AUTH_DETAIL | JWT |
| 6 | GET | `/api/v1/admin/licenses/{id}/verify` | IAS_AUTH_TAMPER_CHECK | JWT |
| 7 | DELETE | `/api/v1/admin/licenses/{id}` | IAS_AUTH_DELETE | JWT |
| 8 | PUT | `/api/v1/admin/licenses/{id}/disable` | IAS_AUTH_DISABLE | JWT |
| 9 | GET | `/api/v1/admin/instances` | IAS_AUTH_INST_LIST | JWT |
| 10 | GET | `/api/v1/admin/instances/offline` | IAS_AUTH_INST_OFFLINE | JWT |
| 11 | GET | `/api/v1/admin/instances/history` | IAS_AUTH_INST_HISTORY | JWT |
| 12 | GET | `/api/v1/admin/instances/{id}` | IAS_AUTH_INST_DETAIL | JWT |
| 13 | GET | `/api/v1/admin/statistics` | IAS_AUTH_STAT_OVERVIEW | JWT |
| 14 | GET | `/api/v1/admin/statistics/trend` | IAS_AUTH_STAT_TREND | JWT |
| 15 | GET | `/api/v1/admin/statistics/dashboard-v2` | IAS_AUTH_DASHBOARD | JWT |
| 16 | GET | `/api/v1/admin/statistics/alerts` | IAS_AUTH_ALERTS | JWT |
| 17 | GET | `/api/v1/admin/statistics/export` | IAS_AUTH_STAT_EXPORT | JWT |
| 18 | GET | `/api/v1/admin/audit-logs` | IAS_AUTH_AUDIT | JWT |
| 19 | GET | `/api/v1/admin/config` | IAS_AUTH_CONFIG_VIEW | JWT |
| 20 | PUT | `/api/v1/admin/config/heartbeat` | IAS_AUTH_CONFIG_HB | JWT |
| 21 | POST | `/api/v1/admin/config/reload` | IAS_AUTH_CONFIG_RELOAD | JWT |

### 2.2 客户端（`/api/v1/license/**`，无鉴权）

| # | 方法 | 路径 | 用例 | 鉴权 |
|---|------|------|------|------|
| 22 | GET | `/api/v1/license/public-key` | IAS_AUTH_PUBLIC_KEY | 无 |
| 23 | GET | `/api/v1/license/heartbeat-config` | IAS_AUTH_HB_CONFIG | 无 |
| 24 | POST | `/api/v1/license/register` | IAS_AUTH_REGISTER | 无 |
| 25 | POST | `/api/v1/license/heartbeat` | IAS_AUTH_HEARTBEAT | 无 |
| 26 | POST | `/api/v1/license/file-apply` | IAS_AUTH_FILE_APPLY | 无 |

### 2.3 健康检查（无鉴权）

| # | 方法 | 路径 | 用例 | 鉴权 |
|---|------|------|------|------|
| 27 | GET | `/api/health` | IAS_AUTH_HEALTH | 无 |

## 3. 统一错误响应

所有失败（4xx/5xx）返回统一格式：

```json
{
  "code": "AUTH_001",
  "message": "登录失败：用户名或密码错误",
  "details": [],
  "timestamp": "2026-09-09T14:30:00Z"
}
```

错误码定义见 `server/src/main/java/com/example/app/exception/ErrorCode.java`（共享契约 `infra:error-codes`）。