// ============================================================
// IAS Auth Center · 管理控制台统一网络层
// 所有 HTTP 请求严格收敛于此，携带 JWT 鉴权头
// ============================================================

import {
  ApiError,
  AuditLog,
  ChangePasswordRequest,
  FileApplyRequest,
  FileApplyResponse,
  HealthResponse,
  HeartbeatConfigRequest,
  HistoryInstance,
  Instance,
  License,
  LicenseImportRequest,
  LicenseSummary,
  LicenseVerifyResponse,
  LoginRequest,
  LoginResponse,
  StatisticsAlert,
  StatisticsDashboard,
  StatisticsOverview,
  StatisticsTrend,
  SystemConfig,
} from '../types'

const TOKEN_KEY = 'ias_auth_token'
const USERNAME_KEY = 'ias_auth_username'

/** 持久化 JWT 令牌 */
export function saveToken(token: string, username: string): void {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USERNAME_KEY, username)
}

/** 读取当前 JWT 令牌 */
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/** 读取当前登录用户名 */
export function getUsername(): string | null {
  return localStorage.getItem(USERNAME_KEY)
}

/** 清除登录态 */
export function clearAuth(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USERNAME_KEY)
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let errorData: ApiError
    try {
      errorData = await res.json()
    } catch {
      errorData = {
        code: 'HTTP_' + res.status,
        message: res.statusText || 'Request failed',
        details: [],
        timestamp: new Date().toISOString(),
      }
    }
    throw errorData
  }
  if (res.status === 204) {
    return {} as T
  }
  return res.json()
}

function authHeaders(extra?: Record<string, string>): Record<string, string> {
  const headers: Record<string, string> = { ...(extra || {}) }
  const token = getToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

export const api = {
  // ------------------------------------------------------------
  // 健康检查（无鉴权）
  // ------------------------------------------------------------
  async getHealth(): Promise<HealthResponse> {
    const res = await fetch('/api/health')
    return handleResponse<HealthResponse>(res)
  },

  // ------------------------------------------------------------
  // 认证 / 管理员
  // ------------------------------------------------------------
  async login(req: LoginRequest): Promise<LoginResponse> {
    const res = await fetch('/api/v1/admin/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    })
    return handleResponse<LoginResponse>(res)
  },

  async changePassword(req: ChangePasswordRequest): Promise<void> {
    const res = await fetch('/api/v1/admin/password', {
      method: 'PUT',
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify(req),
    })
    await handleResponse<void>(res)
  },

  // ------------------------------------------------------------
  // 授权管理
  // ------------------------------------------------------------
  async importLicense(req: LicenseImportRequest): Promise<License> {
    const res = await fetch('/api/v1/admin/licenses/import', {
      method: 'POST',
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify(req),
    })
    return handleResponse<License>(res)
  },

  async listLicenses(): Promise<LicenseSummary[]> {
    const res = await fetch('/api/v1/admin/licenses', { headers: authHeaders() })
    return handleResponse<LicenseSummary[]>(res)
  },

  async getLicense(id: number): Promise<License> {
    const res = await fetch(`/api/v1/admin/licenses/${id}`, { headers: authHeaders() })
    return handleResponse<License>(res)
  },

  async verifyLicense(id: number): Promise<LicenseVerifyResponse> {
    const res = await fetch(`/api/v1/admin/licenses/${id}/verify`, { headers: authHeaders() })
    return handleResponse<LicenseVerifyResponse>(res)
  },

  async deleteLicense(id: number): Promise<void> {
    const res = await fetch(`/api/v1/admin/licenses/${id}`, {
      method: 'DELETE',
      headers: authHeaders(),
    })
    await handleResponse<void>(res)
  },

  async disableLicense(id: number): Promise<License> {
    const res = await fetch(`/api/v1/admin/licenses/${id}/disable`, {
      method: 'PUT',
      headers: authHeaders(),
    })
    return handleResponse<License>(res)
  },

  // ------------------------------------------------------------
  // 实例监控
  // ------------------------------------------------------------
  async listOnlineInstances(): Promise<Instance[]> {
    const res = await fetch('/api/v1/admin/instances', { headers: authHeaders() })
    return handleResponse<Instance[]>(res)
  },

  async listOfflineInstances(): Promise<Instance[]> {
    const res = await fetch('/api/v1/admin/instances/offline', { headers: authHeaders() })
    return handleResponse<Instance[]>(res)
  },

  async listHistoryInstances(): Promise<HistoryInstance[]> {
    const res = await fetch('/api/v1/admin/instances/history', { headers: authHeaders() })
    return handleResponse<HistoryInstance[]>(res)
  },

  async getInstance(id: number): Promise<Instance> {
    const res = await fetch(`/api/v1/admin/instances/${id}`, { headers: authHeaders() })
    return handleResponse<Instance>(res)
  },

  // ------------------------------------------------------------
  // 审计日志
  // ------------------------------------------------------------
  async listAuditLogs(params?: {
    operationType?: string
    operator?: string
    result?: string
    targetId?: string
  }): Promise<AuditLog[]> {
    const qs = new URLSearchParams()
    if (params?.operationType) qs.set('operationType', params.operationType)
    if (params?.operator) qs.set('operator', params.operator)
    if (params?.result) qs.set('result', params.result)
    if (params?.targetId) qs.set('targetId', params.targetId)
    const query = qs.toString()
    const res = await fetch(`/api/v1/admin/audit-logs${query ? `?${query}` : ''}`, {
      headers: authHeaders(),
    })
    return handleResponse<AuditLog[]>(res)
  },

  // ------------------------------------------------------------
  // 系统配置
  // ------------------------------------------------------------
  async getConfig(): Promise<SystemConfig[]> {
    const res = await fetch('/api/v1/admin/config', { headers: authHeaders() })
    return handleResponse<SystemConfig[]>(res)
  },

  async updateHeartbeatConfig(req: HeartbeatConfigRequest): Promise<SystemConfig[]> {
    const res = await fetch('/api/v1/admin/config/heartbeat', {
      method: 'PUT',
      headers: authHeaders({ 'Content-Type': 'application/json' }),
      body: JSON.stringify(req),
    })
    return handleResponse<SystemConfig[]>(res)
  },

  async reloadConfig(): Promise<SystemConfig[]> {
    const res = await fetch('/api/v1/admin/config/reload', {
      method: 'POST',
      headers: authHeaders(),
    })
    return handleResponse<SystemConfig[]>(res)
  },

  // ------------------------------------------------------------
  // 统计与仪表盘
  // ------------------------------------------------------------
  async getStatisticsOverview(): Promise<StatisticsOverview> {
    const res = await fetch('/api/v1/admin/statistics', { headers: authHeaders() })
    return handleResponse<StatisticsOverview>(res)
  },

  async getStatisticsTrend(days = 30): Promise<StatisticsTrend> {
    const res = await fetch(`/api/v1/admin/statistics/trend?days=${days}`, {
      headers: authHeaders(),
    })
    return handleResponse<StatisticsTrend>(res)
  },

  async getDashboard(): Promise<StatisticsDashboard> {
    const res = await fetch('/api/v1/admin/statistics/dashboard-v2', { headers: authHeaders() })
    return handleResponse<StatisticsDashboard>(res)
  },

  async getAlerts(): Promise<StatisticsAlert[]> {
    const res = await fetch('/api/v1/admin/statistics/alerts', { headers: authHeaders() })
    return handleResponse<StatisticsAlert[]>(res)
  },

  async exportStatistics(): Promise<string> {
    const res = await fetch('/api/v1/admin/statistics/export', { headers: authHeaders() })
    if (!res.ok) {
      await handleResponse<never>(res)
    }
    return res.text()
  },

  // ------------------------------------------------------------
  // 授权文件申请（local/site）
  // ------------------------------------------------------------
  async fileApply(req: FileApplyRequest): Promise<FileApplyResponse> {
    const res = await fetch('/api/v1/license/file-apply', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(req),
    })
    return handleResponse<FileApplyResponse>(res)
  },
}