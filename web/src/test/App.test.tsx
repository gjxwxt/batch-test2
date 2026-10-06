import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { App } from '../App'
import { api, clearAuth, getToken, saveToken } from '../services/api'

describe('API service unit tests', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('fetches health status from /api/v1/health', async () => {
    const mockHealth = { status: 'UP', timestamp: '2026-09-09T12:00:00Z', service: 'server' }
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockHealth,
    })

    const result = await api.getHealth()
    expect(result.status).toBe('UP')
    expect(result.service).toBe('server')
    expect(globalThis.fetch).toHaveBeenCalledWith('/api/v1/health')
  })

  it('logs in via POST /api/v1/admin/login and returns token', async () => {
    const mockLogin = { token: 'jwt-token-123', username: 'admin', tokenType: 'Bearer' }
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockLogin,
    })

    const result = await api.login({ username: 'admin', password: 'Admin@123456' })
    expect(result.token).toBe('jwt-token-123')
    expect(result.username).toBe('admin')
    expect(globalThis.fetch).toHaveBeenCalledWith('/api/v1/admin/login', expect.objectContaining({
      method: 'POST',
    }))
  })

  it('lists licenses with JWT auth header', async () => {
    const mockLicenses = [
      { id: 1, serial: 'LIC-2026-0001', licenseName: '企业版', status: 'ACTIVE', maxInstances: 200, usedInstances: 86 },
    ]
    saveToken('jwt-token-123', 'admin')
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockLicenses,
    })

    const result = await api.listLicenses()
    expect(result).toHaveLength(1)
    expect(result[0].serial).toBe('LIC-2026-0001')
    const fetchMock = globalThis.fetch as unknown as ReturnType<typeof vi.fn>
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(init.headers).toEqual({ Authorization: 'Bearer jwt-token-123' })
  })

  it('throws ApiError on non-ok response', async () => {
    const mockError = { code: 'AUTH_001', message: '用户名或密码错误', details: [], timestamp: '2026-09-09T12:00:00Z' }
    globalThis.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => mockError,
    })

    await expect(api.login({ username: 'admin', password: 'wrong' })).rejects.toMatchObject({
      code: 'AUTH_001',
    })
  })

  it('persists and clears auth token', () => {
    saveToken('jwt-token-123', 'admin')
    expect(getToken()).toBe('jwt-token-123')
    clearAuth()
    expect(getToken()).toBeNull()
  })
})

describe('App Component RTL Integration Tests', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('renders login screen when not authenticated', async () => {
    render(<App />)
    expect(await screen.findByText('登录管理控制台')).toBeInTheDocument()
    expect(screen.getByLabelText('用户名')).toBeInTheDocument()
    expect(screen.getByLabelText('密码')).toBeInTheDocument()
  })

  it('logs in and shows dashboard', async () => {
    const user = userEvent.setup()
    vi.spyOn(api, 'login').mockResolvedValue({
      token: 'jwt-token-123',
      username: 'admin',
      tokenType: 'Bearer',
    })
    vi.spyOn(api, 'getDashboard').mockResolvedValue({
      overview: {
        licenseCount: 5,
        activeLicenseCount: 4,
        disabledLicenseCount: 1,
        expiredLicenseCount: 0,
        instanceCount: 12,
        onlineInstanceCount: 10,
        offlineInstanceCount: 2,
        totalCpus: 500,
        totalMemory: 102400,
        usedCpus: 300,
        usedMemory: 51200,
        auditLogCount: 15,
      },
      trend: { days: [] },
      cpuUtilization: 60,
      memoryUtilization: 50,
      alerts: [],
    })

    render(<App />)

    const usernameInput = await screen.findByLabelText('用户名')
    const passwordInput = screen.getByLabelText('密码')
    await user.type(usernameInput, 'admin')
    await user.type(passwordInput, 'Admin@123456')
    await user.click(screen.getByRole('button', { name: /登\s*录/i }))

    await waitFor(() => {
      expect(screen.queryByText('登录管理控制台')).not.toBeInTheDocument()
    })
    expect(await screen.findByText('授权中心运行总览')).toBeInTheDocument()
  })

  it('shows error on invalid login', async () => {
    const user = userEvent.setup()
    vi.spyOn(api, 'login').mockRejectedValue({
      code: 'AUTH_001',
      message: '用户名或密码错误',
      details: [],
      timestamp: '2026-09-09T12:00:00Z',
    })

    render(<App />)

    const usernameInput = await screen.findByLabelText('用户名')
    const passwordInput = screen.getByLabelText('密码')
    await user.type(usernameInput, 'admin')
    await user.type(passwordInput, 'wrong-password')
    await user.click(screen.getByRole('button', { name: /登\s*录/i }))

    expect(await screen.findByRole('alert')).toHaveTextContent('用户名或密码错误')
  })

  it('renders license management page with list', async () => {
    const user = userEvent.setup()
    saveToken('jwt-token-123', 'admin')
    vi.spyOn(api, 'getDashboard').mockResolvedValue({
      overview: {
        licenseCount: 1,
        activeLicenseCount: 1,
        disabledLicenseCount: 0,
        expiredLicenseCount: 0,
        instanceCount: 0,
        onlineInstanceCount: 0,
        offlineInstanceCount: 0,
        totalCpus: 0,
        totalMemory: 0,
        usedCpus: 0,
        usedMemory: 0,
        auditLogCount: 0,
      },
      trend: { days: [] },
      cpuUtilization: 0,
      memoryUtilization: 0,
      alerts: [],
    })
    vi.spyOn(api, 'listLicenses').mockResolvedValue([
      {
        id: 1,
        serial: 'LIC-2026-0001',
        licenseName: '企业版',
        proname: 'InforSuite AS',
        component: 'as',
        version: '9.1.2',
        licensee: '示例客户',
        licenseMode: 'center',
        formal: 'true',
        expiration: '2027-09-12',
        status: 'ACTIVE',
        maxInstances: 200,
        usedInstances: 86,
        remainingInstances: 114,
        createTime: '2026-09-12T10:24:00Z',
        updateTime: '2026-09-12T10:24:00Z',
      },
    ])

    render(<App />)

    await screen.findByText('授权中心运行总览')
    await user.click(screen.getByText('授权管理'))

    expect(await screen.findByText('LIC-2026-0001')).toBeInTheDocument()
    expect(screen.getByText('导入授权')).toBeInTheDocument()
  })

  it('handles backend offline gracefully on dashboard', async () => {
    saveToken('jwt-token-123', 'admin')
    vi.spyOn(api, 'getDashboard').mockRejectedValue(new Error('Network error'))

    render(<App />)

    expect(await screen.findByText(/加载仪表盘失败|Network error/)).toBeInTheDocument()
  })
})