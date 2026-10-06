import React, { useState } from 'react'
import { Icon } from '../components/Icon'
import { api, saveToken } from '../services/api'

interface LoginScreenProps {
  onLogin: (username: string) => void
}

/** 登录页（IAS_AUTH_LOGIN） */
export const LoginScreen: React.FC<LoginScreenProps> = ({ onLogin }) => {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [showPwd, setShowPwd] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (loading) return
    if (!username.trim() || !password) {
      setError('请输入用户名和密码')
      return
    }
    setLoading(true)
    setError(null)
    try {
      const res = await api.login({ username: username.trim(), password })
      saveToken(res.token, res.username)
      onLogin(res.username)
    } catch (err) {
      const msg = (err as { message?: string })?.message || '用户名或密码错误，请重试'
      setError(msg)
      setLoading(false)
    }
  }

  const features = [
    { icon: 'shield', t: '三层防篡改校验', d: '签名验签 · 字段修复 · 配额守恒交叉验证' },
    { icon: 'instance', t: '实例心跳保活', d: '弹性配额判定与超时下线自动归档' },
    { icon: 'audit', t: '全链路审计追溯', d: '注册、保活、配置变更全量留痕' },
  ]

  return (
    <div className="login-wrap">
      <aside className="login-brand">
        <div className="login-brand-top">
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div
              style={{
                width: 38,
                height: 38,
                borderRadius: 9,
                background: 'var(--accent)',
                display: 'grid',
                placeItems: 'center',
                color: '#fff',
              }}
            >
              <Icon name="shield" size={22} />
            </div>
            <div>
              <div style={{ fontWeight: 700, fontSize: 16, lineHeight: 1.3 }}>IAS Auth Center</div>
              <div style={{ fontSize: 12, color: 'oklch(74% 0.02 165)' }}>授权中心 · Center 模式</div>
            </div>
          </div>
        </div>
        <div className="login-brand-mid">
          <div className="login-brand-kicker">INFOR SUITE AS · 授权管理控制台</div>
          <h1 className="login-headline">
            为应用服务器
            <br />
            提供可信授权中枢
          </h1>
          <p className="login-brand-desc">
            统一管理授权导入、实例注册与心跳保活，三层防篡改校验保障授权链路安全，全量审计日志支持合规追溯。
          </p>
          <div className="login-features">
            {features.map((f) => (
              <div key={f.t} className="login-feature">
                <div className="lf-ico">
                  <Icon name={f.icon} size={17} />
                </div>
                <div>
                  <div className="lf-t">{f.t}</div>
                  <div className="lf-d">{f.d}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
        <div className="login-brand-foot">
          <span>© 2026 InforSuite AS</span>
          <span className="mono" style={{ fontSize: 11.5 }}>
            v9.1 · RSA-2048 / SHA256
          </span>
        </div>
      </aside>

      <main className="login-form-side">
        <div className="login-card">
          <div className="card login-card-box">
            <h2 className="login-title">登录管理控制台</h2>
            <p className="login-sub">管理授权、实例、审计与系统配置</p>
            <form onSubmit={submit} noValidate>
              <div style={{ marginBottom: 14 }}>
                <label className="field-label" htmlFor="login-username">
                  用户名
                </label>
                <input
                  id="login-username"
                  className="input"
                  value={username}
                  onChange={(e) => {
                    setUsername(e.target.value)
                    if (error) setError(null)
                  }}
                  placeholder="请输入用户名"
                  autoComplete="username"
                />
              </div>
              <div>
                <label className="field-label" htmlFor="login-password">
                  密码
                </label>
                <div className="pwd-wrap">
                  <input
                    id="login-password"
                    className="input"
                    type={showPwd ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value)
                      if (error) setError(null)
                    }}
                    placeholder="请输入密码"
                    autoComplete="current-password"
                  />
                  <button
                    type="button"
                    className="pwd-toggle"
                    onClick={() => setShowPwd((v) => !v)}
                    aria-label={showPwd ? '隐藏密码' : '显示密码'}
                  >
                    <Icon name={showPwd ? 'eye-off' : 'eye'} size={16} />
                  </button>
                </div>
              </div>
              {error && (
                <div className="login-error" role="alert">
                  <Icon name="alert" size={15} style={{ flex: 'none', marginTop: 1 }} />
                  <span>{error}</span>
                </div>
              )}
              <button className="btn btn-primary login-submit" type="submit" disabled={loading}>
                {loading ? (
                  <>
                    <span className="spin" aria-hidden="true" />
                    验证中…
                  </>
                ) : (
                  '登 录'
                )}
              </button>
            </form>
          </div>
        </div>
        <div className="login-foot">登录即代表同意《服务条款》与《隐私政策》</div>
      </main>
    </div>
  )
}