# 共享契约 · API 骨架（infra:api-skeleton）

> 冻结基线（wp-0）。路径/鉴权/错误语义为所有并行工作包的 HTTP 接缝。
> 需求来源：`center模式-服务端需求文档(1).md` 6.2 节 Use Case → API 路径映射。

## 路径前缀

- `/api/v1/admin/**` — 管理控制台接口，**JWT 鉴权**（除 login）。
- `/api/v1/license/**` — 客户端 SDK 接口，**无鉴权**（注册/心跳/公钥/心跳配置/文件申请）。
- `/api/v1/health` — 健康检查，无鉴权。

## 端点清单

### 认证 / 管理员（admin）
| Use Case | 方法 | 路径 | 鉴权 |
|----------|------|------|------|
| IAS_AUTH_LOGIN | POST | /api/v1/admin/login | 无 |
| IAS_AUTH_CHANGE_PWD | PUT | /api/v1/admin/password | JWT |

### 授权管理（admin）
| Use Case | 方法 | 路径 | 鉴权 |
|----------|------|------|------|
| IAS_AUTH_IMPORT | POST | /api/v1/admin/licenses/import | JWT |
| IAS_AUTH_TAMPER_CHECK | GET | /api/v1/admin/licenses/{id}/verify | JWT |
| IAS_AUTH_DELETE | DELETE | /api/v1/admin/licenses/{id} | JWT |
| IAS_AUTH_DISABLE | PUT | /api/v1/admin/licenses/{id}/disable | JWT |
| IAS_AUTH_LIST | GET | /api/v1/admin/licenses | JWT |
| IAS_AUTH_DETAIL | GET | /api/v1/admin/licenses/{id} | JWT |

### 客户端交互（license，无鉴权）
| Use Case | 方法 | 路径 | 鉴权 |
|----------|------|------|------|
| IAS_AUTH_PUBLIC_KEY | GET | /api/v1/license/public-key | 无 |
| IAS_AUTH_HB_CONFIG | GET | /api/v1/license/heartbeat-config | 无 |
| IAS_AUTH_REGISTER | POST | /api/v1/license/register | 无 |
| IAS_AUTH_HEARTBEAT | POST | /api/v1/license/heartbeat | 无 |
| IAS_AUTH_FILE_APPLY | POST | /api/v1/license/file-apply | 无 |

### 实例监控（admin）
| Use Case | 方法 | 路径 | 鉴权 |
|----------|------|------|------|
| IAS_AUTH_INST_LIST | GET | /api/v1/admin/instances | JWT |
| IAS_AUTH_INST_OFFLINE | GET | /api/v1/admin/instances/offline | JWT |
| IAS_AUTH_INST_HISTORY | GET | /api/v1/admin/instances/history | JWT |
| IAS_AUTH_INST_DETAIL | GET | /api/v1/admin/instances/{id} | JWT |

### 统计与仪表盘（admin，O2 纳入）
| Use Case | 方法 | 路径 | 鉴权 |
|----------|------|------|------|
| IAS_AUTH_STAT_OVERVIEW | GET | /api/v1/admin/statistics | JWT |
| IAS_AUTH_STAT_TREND | GET | /api/v1/admin/statistics/trend | JWT |
| IAS_AUTH_DASHBOARD | GET | /api/v1/admin/statistics/dashboard-v2 | JWT |
| IAS_AUTH_ALERTS | GET | /api/v1/admin/statistics/alerts | JWT |
| IAS_AUTH_STAT_EXPORT | GET | /api/v1/admin/statistics/export | JWT |

### 审计 / 配置（admin）
| Use Case | 方法 | 路径 | 鉴权 |
|----------|------|------|------|
| IAS_AUTH_AUDIT | GET | /api/v1/admin/audit-logs | JWT |
| IAS_AUTH_CONFIG_VIEW | GET | /api/v1/admin/config | JWT |
| IAS_AUTH_CONFIG_HB | PUT | /api/v1/admin/config/heartbeat | JWT |
| IAS_AUTH_CONFIG_RELOAD | POST | /api/v1/admin/config/reload | JWT |

### 健康检查
| Use Case | 方法 | 路径 | 鉴权 |
|----------|------|------|------|
| IAS_AUTH_HEALTH | GET | /api/v1/health | 无 |

## 鉴权语义

- 管理接口携带 `Authorization: Bearer <JWT>`；JWT 过期 → `AUTH_002`（HTTP 401）。
- 客户端接口无鉴权，但注册/心跳请求体含签名（见 infra:signature 契约）。

## 错误语义

- 统一 `ApiErrorResponse`（见 infra:error-codes）。HTTP 状态码映射：
  - 400 → PARAM_001 / USER_003 / USER_004 / LICENSE_005
  - 401 → AUTH_001 / AUTH_002
  - 404 → LICENSE_001 / USER_001 / INSTANCE_002
  - 409 → LICENSE_006
  - 500 → SYS_001

## 变更控制

路径/鉴权为冻结契约。新增端点须经 contract_review 评审。