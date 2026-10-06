# RESTful API 规范与契约 (API Specification)

## 1. 基础约定

- **根路径**：业务 API 统一使用 `/api/v1` 前缀；健康检查与平台契约使用 `/api/health`。
- **传输编码**：所有请求与响应体统一采用 UTF-8 编码的 JSON 格式（`Content-Type: application/json`）。
- **时间格式**：采用 ISO 8601 字符串格式，如 `2026-09-09T14:30:00Z`。

---

## 2. 统一健康检查契约

### `GET /api/health`
系统健康状态探针，供容器健康检查与 Multigent 预览引擎探活使用。

#### 响应示例 (200 OK)
```json
{
  "status": "UP",
  "timestamp": "2026-09-09T14:30:00Z",
  "service": "server"
}
```

---

## 3. 示例业务接口：Item 管理

### 3.1 查询列表
- **路径**：`GET /api/v1/items`
- **响应 (200 OK)**：
```json
[
  {
    "id": "item-1",
    "title": "Initial Task",
    "description": "Baseline task created during bootstrap",
    "status": "PENDING",
    "createdAt": "2026-09-09T14:30:00Z"
  }
]
```

### 3.2 创建 Item
- **路径**：`POST /api/v1/items`
- **请求体**：
```json
{
  "title": "New Task",
  "description": "Task description"
}
```
- **字段约束**：`title` 不能为空（`@NotBlank`），最大 100 字符。
- **响应 (201 Created)**：创建成功的 Item 对象。

### 3.3 根据 ID 查询
- **路径**：`GET /api/v1/items/{id}`
- **响应 (200 OK)**：Item 对象。
- **响应 (404 Not Found)**：当 ID 不存在时返回标准错误响应。

### 3.4 删除 Item
- **路径**：`DELETE /api/v1/items/{id}`
- **响应 (204 No Content)**：删除成功。

---

## 3.5 授权管理 (License Admin) — wp-3

授权管理端点，路径前缀 `/api/v1/admin/licenses`（JWT 鉴权）。

### 3.5.1 导入授权
- **路径**：`POST /api/v1/admin/licenses/import`
- **请求体**：
```json
{
  "licenseFile": "<?xml version=\"1.0\"...><license>...</license>",
  "licenseName": "My License"
}
```
- **字段约束**：`licenseFile` 不能为空（`@NotBlank`）。
- **响应 (201 Created)**：导入后的授权对象。
- **错误**：`LICENSE_002`（签名验证失败，400）、`LICENSE_004`（已过期，400）、`LICENSE_005`（重复导入，400）。

### 3.5.2 授权列表
- **路径**：`GET /api/v1/admin/licenses`
- **响应 (200 OK)**：授权概要数组（不含完整授权文件）。

### 3.5.3 授权详情
- **路径**：`GET /api/v1/admin/licenses/{id}`
- **响应 (200 OK)**：授权对象。
- **错误**：`LICENSE_001`（授权不存在，404）。

### 3.5.4 三层防篡改校验
- **路径**：`GET /api/v1/admin/licenses/{id}/verify`
- **响应 (200 OK)**：
```json
{
  "serial": "serial-001",
  "verified": true,
  "layers": [
    { "name": "SIGNATURE", "passed": true, "detail": "..." },
    { "name": "DB_STATE", "passed": true, "detail": "..." },
    { "name": "FILE_INTEGRITY", "passed": true, "detail": "..." }
  ],
  "message": "授权校验通过"
}
```
- 三层：数字签名（RSA-2048 + SHA256withRSA）、数据库状态、存储文件一致性。

### 3.5.5 删除授权
- **路径**：`DELETE /api/v1/admin/licenses/{id}`
- **响应 (204 No Content)**：删除成功。
- **错误**：`LICENSE_006`（存在在线实例，409）。

### 3.5.6 禁用授权
- **路径**：`PUT /api/v1/admin/licenses/{id}/disable`
- **响应 (200 OK)**：禁用后的授权对象（`status=DISABLED`）。

---

## 4. 统一错误响应格式 (Uniform Error Response)

当请求失败（4xx 或 5xx）时，服务端严格返回统一格式的 JSON：

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Item with id 'item-999' was not found",
  "details": [],
  "timestamp": "2026-09-09T14:30:00Z"
}
```

#### 参数校验失败示例 (400 Bad Request)
```json
{
  "code": "VALIDATION_FAILED",
  "message": "Validation failed for 1 fields",
  "details": [
    "title: must not be blank"
  ],
  "timestamp": "2026-09-09T14:30:00Z"
}
```
