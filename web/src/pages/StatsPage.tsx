import React, { useEffect, useState } from 'react'
import { Icon } from '../components/Icon'
import { EmptyState, Loading } from '../components/ui'
import { api } from '../services/api'
import { StatisticsOverview, StatisticsTrend } from '../types'

interface StatsPageProps {
  notify: (msg: string) => void
}

/** 统计报表（IAS_AUTH_STAT_OVERVIEW / TREND / EXPORT） */
export const StatsPage: React.FC<StatsPageProps> = ({ notify }) => {
  const [overview, setOverview] = useState<StatisticsOverview | null>(null)
  const [trend, setTrend] = useState<StatisticsTrend | null>(null)
  const [loading, setLoading] = useState(true)

  const load = async () => {
    setLoading(true)
    try {
      const [ov, tr] = await Promise.all([api.getStatisticsOverview(), api.getStatisticsTrend(30)])
      setOverview(ov)
      setTrend(tr)
    } catch (err) {
      notify((err as { message?: string })?.message || '加载统计数据失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const exportCsv = async () => {
    try {
      const csv = await api.exportStatistics()
      const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `ias-auth-statistics-${new Date().toISOString().slice(0, 10)}.csv`
      a.click()
      URL.revokeObjectURL(url)
      notify('统计报表已导出')
    } catch (err) {
      notify((err as { message?: string })?.message || '导出失败')
    }
  }

  if (loading) return <Loading text="加载统计报表…" />

  if (!overview) {
    return (
      <div className="card">
        <EmptyState icon="stats" title="暂无统计数据" />
      </div>
    )
  }

  const o = overview
  const stats = [
    { label: '授权总数', value: o.licenseCount, sub: `${o.activeLicenseCount} 启用` },
    { label: '实例总数', value: o.instanceCount, sub: `${o.onlineInstanceCount} 在线` },
    { label: 'CPU 配额', value: `${o.usedCpus}/${o.totalCpus}`, sub: '已用/总量（核）' },
    { label: '内存配额', value: `${o.usedMemory}/${o.totalMemory}`, sub: '已用/总量（MB）' },
    { label: '审计日志', value: o.auditLogCount, sub: '累计记录' },
  ]

  const maxReg = trend?.days.length ? Math.max(...trend.days.map((d) => d.instanceCount), 1) : 1
  const maxLic = trend?.days.length ? Math.max(...trend.days.map((d) => d.licenseCount), 1) : 1

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>统计报表</h2>
          <div className="sub">授权、实例与资源配额统计</div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-ghost btn-sm" onClick={load}>
            <Icon name="refresh" size={14} />
            刷新
          </button>
          <button className="btn btn-primary btn-sm" onClick={exportCsv}>
            <Icon name="download" size={14} />
            导出 CSV
          </button>
        </div>
      </div>

      <div className="stat-grid">
        {stats.map((s) => (
          <div key={s.label} className="card stat-card">
            <div className="stat-label">{s.label}</div>
            <div className="stat-value">{s.value}</div>
            <div className="stat-sub">{s.sub}</div>
          </div>
        ))}
      </div>

      <div className="card" style={{ padding: 18, marginTop: 16 }}>
        <div style={{ fontWeight: 650, marginBottom: 16 }}>近 30 天趋势</div>
        {!trend || trend.days.length === 0 ? (
          <EmptyState icon="stats" title="暂无趋势数据" />
        ) : (
          <div>
            <div style={{ fontSize: 12.5, color: 'var(--muted)', marginBottom: 8 }}>实例注册数</div>
            <div style={{ display: 'flex', alignItems: 'flex-end', gap: 3, height: 120 }}>
              {trend.days.map((d, i) => (
                <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
                  <div
                    className="bar-fill"
                    style={{
                      width: '100%',
                      height: `${Math.max(2, (d.instanceCount / maxReg) * 100)}px`,
                      background: 'var(--accent)',
                    }}
                  />
                </div>
              ))}
            </div>
            <div style={{ fontSize: 12.5, color: 'var(--muted)', margin: '16px 0 8px' }}>授权导入数</div>
            <div style={{ display: 'flex', alignItems: 'flex-end', gap: 3, height: 120 }}>
              {trend.days.map((d, i) => (
                <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
                  <div
                    className="bar-fill"
                    style={{
                      width: '100%',
                      height: `${Math.max(2, (d.licenseCount / maxLic) * 100)}px`,
                      background: 'oklch(58% 0.16 240)',
                    }}
                  />
                </div>
              ))}
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 8 }}>
              <span className="mono" style={{ fontSize: 11, color: 'var(--muted)' }}>
                {trend.days[0]?.date}
              </span>
              <span className="mono" style={{ fontSize: 11, color: 'var(--muted)' }}>
                {trend.days[trend.days.length - 1]?.date}
              </span>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}