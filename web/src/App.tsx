import React, { useCallback, useState } from 'react'
import { Icon } from './components/Icon'
import { Toast } from './components/ui'
import { ChangePasswordModal } from './components/ChangePasswordModal'
import { LoginScreen } from './pages/LoginScreen'
import { Dashboard } from './pages/Dashboard'
import { LicensePage } from './pages/LicensePage'
import { InstancePage } from './pages/InstancePage'
import { AuditPage } from './pages/AuditPage'
import { StatsPage } from './pages/StatsPage'
import { ApplyPage } from './pages/ApplyPage'
import { ConfigPage } from './pages/ConfigPage'
import { clearAuth, getToken, getUsername } from './services/api'

type PageId = 'dashboard' | 'license' | 'instance' | 'audit' | 'stats' | 'apply' | 'config'

const NAV: { id: PageId; label: string; icon: string }[] = [
  { id: 'dashboard', label: '仪表盘', icon: 'dashboard' },
  { id: 'license', label: '授权管理', icon: 'license' },
  { id: 'instance', label: '实例监控', icon: 'instance' },
  { id: 'audit', label: '审计日志', icon: 'audit' },
  { id: 'stats', label: '统计报表', icon: 'stats' },
  { id: 'apply', label: '授权文件申请', icon: 'apply' },
  { id: 'config', label: '系统配置', icon: 'config' },
]

const PAGE_TITLES: Record<PageId, string> = {
  dashboard: '仪表盘',
  license: '授权管理',
  instance: '实例监控',
  audit: '审计日志',
  stats: '统计报表',
  apply: '授权文件申请',
  config: '系统配置',
}

export function App() {
  const [user, setUser] = useState<string | null>(getUsername())
  const [page, setPage] = useState<PageId>('dashboard')
  const [toast, setToast] = useState<string | null>(null)
  const [pwdOpen, setPwdOpen] = useState(false)

  const notify = useCallback((msg: string) => setToast(msg), [])

  const handleLogin = (username: string) => {
    setUser(username)
    setPage('dashboard')
  }

  const handleLogout = () => {
    clearAuth()
    setUser(null)
  }

  if (!user || !getToken()) {
    return <LoginScreen onLogin={handleLogin} />
  }

  const go = (p: PageId) => setPage(p)

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="brand-mark">
            <Icon name="shield" size={20} />
          </div>
          <div>
            <div style={{ fontWeight: 700, fontSize: 14, lineHeight: 1.2 }}>IAS Auth Center</div>
            <div style={{ fontSize: 11, color: 'var(--muted)' }}>授权中心 · Center 模式</div>
          </div>
        </div>
        <nav className="sidebar-nav">
          {NAV.map((n) => (
            <div
              key={n.id}
              className={`nav-item ${page === n.id ? 'active' : ''}`}
              onClick={() => go(n.id)}
            >
              <Icon name={n.icon} size={18} />
              <span>{n.label}</span>
            </div>
          ))}
        </nav>
        <div className="sidebar-foot">
          <div className="mono" style={{ fontSize: 11 }}>
            v9.1 · RSA-2048 / SHA256
          </div>
        </div>
      </aside>

      <main className="main">
        <div className="topbar">
          <div className="topbar-title">{PAGE_TITLES[page]}</div>
          <div className="topbar-right">
            <span style={{ fontSize: 12, color: 'var(--muted)' }}>Center 模式 · 授权中心</span>
            <button className="btn btn-ghost btn-sm" onClick={() => setPwdOpen(true)}>
              <Icon name="lock" size={14} />
              修改密码
            </button>
            <span style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 13 }}>
              <Icon name="user" size={15} />
              {user}
            </span>
            <button className="btn btn-ghost btn-sm" onClick={handleLogout} title="退出登录" aria-label="退出登录">
              <Icon name="logout" size={16} />
            </button>
          </div>
        </div>
        <div className="content">
          {page === 'dashboard' && <Dashboard notify={notify} go={go} />}
          {page === 'license' && <LicensePage notify={notify} />}
          {page === 'instance' && <InstancePage notify={notify} />}
          {page === 'audit' && <AuditPage notify={notify} />}
          {page === 'stats' && <StatsPage notify={notify} />}
          {page === 'apply' && <ApplyPage notify={notify} />}
          {page === 'config' && <ConfigPage notify={notify} />}
        </div>
      </main>

      {pwdOpen && <ChangePasswordModal onClose={() => setPwdOpen(false)} notify={notify} />}
      {toast && <Toast msg={toast} onClose={() => setToast(null)} />}
    </div>
  )
}