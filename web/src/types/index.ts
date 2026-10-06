// ============================================================
// IAS Auth Center · 管理控制台前端类型定义
// 与后端 DTO（server/src/main/java/com/example/app/model/*）对齐
// ============================================================

/** 统一错误响应（ApiErrorResponse） */
export interface ApiError {
  code: string
  message: string
  details: string[]
  timestamp: string
}

/** 健康检查响应 */
export interface HealthResponse {
  status: string
  timestamp: string
  service: string
}

// ------------------------------------------------------------
// 认证 / 管理员（IAS_AUTH_LOGIN / IAS_AUTH_CHANGE_PWD）
// ------------------------------------------------------------

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  username: string
  tokenType: string
}

export interface ChangePasswordRequest {
  oldPassword: string
  newPassword: string
}

// ------------------------------------------------------------
// 授权管理（IAS_AUTH_IMPORT / LIST / DETAIL / VERIFY / DELETE / DISABLE）
// ------------------------------------------------------------

export interface LicenseImportRequest {
  licenseFile: string
  licenseName?: string
}

/** 授权详情（License） */
export interface License {
  id: number
  serial: string
  licenseName: string
  proname: string
  component: string
  version: string
  licensee: string
  licenseMode: string
  formal: string
  expiration: string
  userinfor: string
  maxInstances: number
  maxCpus: number | null
  maxMemory: number | null
  usedInstances: number
  remainingInstances: number
  usedCpus: number
  usedMemory: number
  bxbFile: string
  status: string
  source: string
  createTime: string
  updateTime: string
}

/** 授权列表项（LicenseSummary） */
export interface LicenseSummary {
  id: number
  serial: string
  licenseName: string
  proname: string
  component: string
  version: string
  licensee: string
  licenseMode: string
  formal: string
  expiration: string
  status: string
  maxInstances: number
  usedInstances: number
  remainingInstances: number
  createTime: string
  updateTime: string
}

/** 授权防篡改校验响应（LicenseVerifyResponse） */
export interface LicenseVerifyResponse {
  serial: string
  verified: boolean
  layers: LicenseVerifyLayer[]
  message: string
}

export interface LicenseVerifyLayer {
  name: string
  passed: boolean
  detail: string
}

// ------------------------------------------------------------
// 实例监控（IAS_AUTH_INST_LIST / OFFLINE / HISTORY / DETAIL）
// ------------------------------------------------------------

/** 在线/下线实例（Instance） */
export interface Instance {
  id: number
  instanceId: string
  licenseId: number
  clientUuid: string
  proname: string
  productType: string
  productVersion: string
  productSpec: string
  hostname: string
  ipAddress: string
  mac: string
  machineType: string
  currentCpus: number
  currentMemory: number
  extendedAttributes: string
  status: string
  onlineTime: string
  lastHeartbeatTime: string
  offlineTime: string
  createTime: string
  updateTime: string
}

/** 历史实例（HistoryInstance） */
export interface HistoryInstance {
  id: number
  instanceId: string
  licenseId: number
  clientUuid: string
  proname: string
  productType: string
  productVersion: string
  productSpec: string
  hostname: string
  ipAddress: string
  mac: string
  machineType: string
  currentCpus: number
  currentMemory: number
  extendedAttributes: string
  status: string
  onlineTime: string
  lastHeartbeatTime: string
  offlineTime: string
  archivedTime: string
  createTime: string
  updateTime: string
}

// ------------------------------------------------------------
// 审计日志（IAS_AUTH_AUDIT）
// ------------------------------------------------------------

export type AuditOperationType =
  | 'LOGIN'
  | 'CHANGE_PASSWORD'
  | 'LICENSE_IMPORT'
  | 'LICENSE_DELETE'
  | 'LICENSE_DISABLE'
  | 'LICENSE_VERIFY'
  | 'LICENSE_QUERY'
  | 'INSTANCE_OFFLINE'
  | 'INSTANCE_QUERY'
  | 'CONFIG_UPDATE'
  | 'CONFIG_RELOAD'
  | 'AUDIT_QUERY'
  | 'STATISTICS_QUERY'

export interface AuditLog {
  logId: string
  operationType: AuditOperationType
  operationDesc: string
  operator: string
  operatorIp: string
  targetId: string
  result: string
  detail: string
  operateTime: string
}

// ------------------------------------------------------------
// 系统配置（IAS_AUTH_CONFIG_VIEW / CONFIG_HB / CONFIG_RELOAD）
// ------------------------------------------------------------

export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  configDesc: string
  updateTime: string
}

export interface HeartbeatConfigRequest {
  heartbeatInterval: number
  timeoutMultiplier: number
}

// ------------------------------------------------------------
// 统计与仪表盘（IAS_AUTH_STAT_OVERVIEW / TREND / DASHBOARD / ALERTS / EXPORT）
// ------------------------------------------------------------

export interface StatisticsOverview {
  licenseCount: number
  activeLicenseCount: number
  disabledLicenseCount: number
  expiredLicenseCount: number
  instanceCount: number
  onlineInstanceCount: number
  offlineInstanceCount: number
  totalCpus: number
  totalMemory: number
  usedCpus: number
  usedMemory: number
  auditLogCount: number
}

export interface TrendPoint {
  date: string
  instanceCount: number
  licenseCount: number
}

export interface StatisticsTrend {
  days: TrendPoint[]
}

export interface StatisticsAlert {
  level: string
  type: string
  message: string
  target: string
}

export interface StatisticsDashboard {
  overview: StatisticsOverview
  trend: StatisticsTrend
  cpuUtilization: number
  memoryUtilization: number
  alerts: StatisticsAlert[]
}

// ------------------------------------------------------------
// 授权文件申请（IAS_AUTH_FILE_APPLY）
// ------------------------------------------------------------

export interface FileApplyRequest {
  mode: string
  product: string
  edition: string
  licensee: string
  maxInstances: number
  expireAt?: string
  remark?: string
}

export interface FileApplyResponse {
  applyId: string
  mode: string
  status: string
  message: string
  licenseFile?: string
}