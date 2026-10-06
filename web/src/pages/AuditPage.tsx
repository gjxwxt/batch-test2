import React, { useEffect, useState } from 'react'
import { Icon } from '../components/Icon'
import { EmptyState, Loading, StatusPill } from '../components/ui'
import { api } from '../services/api'
import { AuditLog, AuditOperationType } from '../types'

interface AuditPageProps {
  notify: (msg: string) => void
}

const OPERATION_TYPES: AuditOperationType[] = [
  'LOGIN',
  'CHANGE_PASSWORD',
  'LICENSE_IMPORT',
  'LICENSE_DELETE',
  'LICENSE_DISABLE',
  'LICENSE_VERIFY',
  'LICENSE_QUERY',
  'INSTANCE_OFFLINE',
  'INSTANCE_QUERY',
  'CONFIG_UPDATE',
  'CONFIG_RELOAD',
  'AUDIT_QUERY',
  'STATISTICS_QUERY',
]

/** 审计日志（IAS_AUTH_AUDIT） */
export const AuditPage: React.FC<AuditPageProps> = ({ notify }) => {
  const [logs, setLogs] = useState<AuditLog[]>([])
  const [loading, setLoading] = useState(true)
  const [operationType, setOperationType] = useState('')
  const [operator, setOperator] = useState('')
  const [result, setResult] = useState('')

  const load = async () => {
    setLoading(true)
    try {
      const list = await api.listAuditLogs({
        operationType: operationType || undefined,
        operator: operator || undefined,
        result: result || undefined,
      })
      setLogs(list)
    } catch (err) {
      notify((err as { message?: string })?.message || '加载审计日志失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>审计日志</h2>
          <div className="sub">全链路操作留痕，支持合规追溯</div>
        </div>
        <button className="btn btn-ghost btn-sm" onClick={load}>
          <Icon name="refresh" size={14} />
          刷新
        </button>
      </div>

      <div className="toolbar">
        <select
          className="select"
          style={{ width: 180 }}
          value={operationType}
          onChange={(e) => setOperationType(e.target.value)}
        >
          <option value="">全部操作类型</option>
          {OPERATION_TYPES.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
        <input
          className="input"
          style={{ width: 160 }}
          placeholder="操作者"
          value={operator}
          onChange={(e) => setOperator(e.target.value)}
        />
        <select
          className="select"
          style={{ width: 120 }}
          value={result}
          onChange={(e) => setResult(e.target.value)}
        >
          <option value="">全部结果</option>
          <option value="success">成功</option>
          <option value="warn">警告</option>
          <option value="fail">失败</option>
        </select>
        <button className="btn btn-ghost btn-sm" onClick={load}>
          查询
        </button>
      </div>

      {loading ? (
        <Loading />
      ) : logs.length === 0 ? (
        <div className="card">
          <EmptyState icon="audit" title="暂无审计日志" desc="当前筛选条件下没有记录" />
        </div>
      ) : (
        <div className="card" style={{ overflow: 'auto' }}>
          <table className="table">
            <thead>
              <tr>
                <th>时间</th>
                <th>操作类型</th>
                <th>操作者</th>
                <th>目标</th>
                <th>结果</th>
                <th>明细</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((l) => (
                <tr key={l.logId}>
                  <td className="mono" style={{ fontSize: 12 }}>
                    {l.operateTime ? new Date(l.operateTime).toLocaleString() : '—'}
                  </td>
                  <td>
                    <span className="mono" style={{ fontSize: 12 }}>
                      {l.operationType}
                    </span>
                  </td>
                  <td>{l.operator}</td>
                  <td className="mono" style={{ fontSize: 12 }}>
                    {l.targetId || '—'}
                  </td>
                  <td>
                    <StatusPill status={l.result === 'success' ? 'success' : l.result === 'warn' ? 'warn' : 'danger'} />
                  </td>
                  <td style={{ fontSize: 12.5 }}>{l.detail}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}