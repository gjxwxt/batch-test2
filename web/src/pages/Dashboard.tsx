import React, { useEffect, useState } from 'react'
import { Icon } from '../components/Icon'
import { Loading, StatusPill } from '../components/ui'
import { api } from '../services/api'
import { StatisticsDashboard } from '../types'

interface DashboardProps {
  notify: (msg: string) => void
  go: (page: 'license' | 'instance') => void
}

/** 仪表盘（IAS_AUTH_DASHBOARD） */
export const Dashboard: React.FC<DashboardProps> = ({ notify, go }) => {
  const [data, setData] = useState<StatisticsDashboard | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = async () => {
    setLoading(true)
    setError(null)
    try {
      const d = await api.getDashboard()
      setData(d)
    } catch (err) {
      setError((err as { message?: string })?.message || '加载仪表盘失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (loading) return <Loading text="加载仪表盘…" />
  if (error || !data) {
    return (
      <div className="card" style={{ padding: 24 }}>
        <div style={{ color: 'oklch(50% 0.18 25)' }}>{error || '暂无数据'}</div>
        <button className="btn btn-ghost btn-sm" style={{ marginTop: 12 }} onClick={load}>
          <Icon name="refresh" size={14} />
          重试
        </button>
      </div>
    )
  }

  const o = data.overview
  const stats = [
    { label: '授权总数', value: o.licenseCount, sub: `${o.activeLicenseCount} 启用`, icon: 'license' },
    { label: '在线实例', value: o.onlineInstanceCount, sub: `共 ${o.instanceCount} 实例`, icon: 'instance' },
    { label: 'CPU 利用率', value: `${Math.round(data.cpuUtilization)}%`, sub: `${o.usedCpus}/${o.totalCpus} 核`, icon: 'stats' },
    { label: '内存利用率', value: `${Math.round(data.memoryUtilization)}%`, sub: `${o.usedMemory}/${o.totalMemory} MB`, icon: 'stats' },
    { label: '审计日志', value: o.auditLogCount, sub: '全链路留痕', icon: 'audit' },
  ]

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>仪表盘</h2>
          <div className="sub">授权中心运行总览</div>
        </div>
        <button className="btn btn-ghost btn-sm" onClick={load}>
          <Icon name="refresh" size={14} />
          刷新
        </button>
      </div>

      <div className="stat-grid">
        {stats.map((s) => (
          <div key={s.label} className="card stat-card">
            <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <Icon name={s.icon} size={16} />
              <span className="stat-label">{s.label}</span>
            </div>
            <div className="stat-value">{s.value}</div>
            <div className="stat-sub">{s.sub}</div>
          </div>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginTop: 16 }}>
        <div className="card" style={{ padding: 18 }}>
          <div style={{ fontWeight: 650, marginBottom: 12 }}>资源利用率</div>
          <div style={{ marginBottom: 14 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12.5, marginBottom: 6 }}>
              <span>CPU</span>
              <span className="mono">{Math.round(data.cpuUtilization)}%</span>
            </div>
            <div className="bar-track">
              <div className="bar-fill" style={{ width: `${Math.min(100, data.cpuUtilization)}%` }} />
            </div>
          </div>
          <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12.5, marginBottom: 6 }}>
              <span>内存</span>
              <span className="mono">{Math.round(data.memoryUtilization)}%</span>
            </div>
            <div className="bar-track">
              <div className="bar-fill" style={{ width: `${Math.min(100, data.memoryUtilization)}%` }} />
            </div>
          </div>
        </div>

        <div className="card" style={{ padding: 18 }}>
          <div style={{ fontWeight: 650, marginBottom: 12 }}>告警</div>
          {data.alerts.length === 0 ? (
            <div style={{ color: 'var(--muted)', fontSize: 13 }}>暂无告警</div>
          ) : (
            <div className="alert-list">
              {data.alerts.slice(0, 5).map((a, i) => (
                <div key={i} className={`alert-item ${a.level.toLowerCase()}`}>
                  <Icon name="alert" size={15} style={{ flex: 'none', marginTop: 1 }} />
                  <span>{a.message}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="card" style={{ padding: 18, marginTop: 16 }}>
        <div style={{ fontWeight: 650, marginBottom: 12 }}>授权状态分布</div>
        <div style={{ display: 'flex', gap: 24, flexWrap: 'wrap' }}>
          <div>
            <StatusPill status="active" /> <span className="mono">{o.activeLicenseCount}</span>
          </div>
          <div>
            <StatusPill status="disabled" /> <span className="mono">{o.disabledLicenseCount}</span>
          </div>
          <div>
            <StatusPill status="expired" /> <span className="mono">{o.expiredLicenseCount}</span>
          </div>
          <div>
            <StatusPill status="offline" /> <span className="mono">{o.offlineInstanceCount}</span>
          </div>
        </div>
        <div style={{ marginTop: 14 }}>
          <button className="btn btn-ghost btn-sm" onClick={() => go('license')}>
            查看授权管理
          </button>
          <button className="btn btn-ghost btn-sm" style={{ marginLeft: 8 }} onClick={() => go('instance')}>
            查看实例监控
          </button>
        </div>
      </div>
    </div>
  )
}