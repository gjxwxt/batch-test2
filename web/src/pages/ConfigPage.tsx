import React, { useEffect, useState } from 'react'
import { Icon } from '../components/Icon'
import { EmptyState, Loading, Modal } from '../components/ui'
import { api } from '../services/api'
import { SystemConfig } from '../types'

interface ConfigPageProps {
  notify: (msg: string) => void
}

/** 系统配置（IAS_AUTH_CONFIG_VIEW / CONFIG_HB / CONFIG_RELOAD） */
export const ConfigPage: React.FC<ConfigPageProps> = ({ notify }) => {
  const [configs, setConfigs] = useState<SystemConfig[]>([])
  const [loading, setLoading] = useState(true)
  const [editOpen, setEditOpen] = useState(false)
  const [draft, setDraft] = useState({ heartbeatInterval: 30, timeoutMultiplier: 3 })

  const load = async () => {
    setLoading(true)
    try {
      const list = await api.getConfig()
      setConfigs(list)
    } catch (err) {
      notify((err as { message?: string })?.message || '加载系统配置失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const getValue = (key: string): string => {
    const c = configs.find((x) => x.configKey === key)
    return c?.configValue ?? '—'
  }

  const startEdit = () => {
    const hb = Number(getValue('heartbeat.interval')) || 30
    const mult = Number(getValue('heartbeat.timeout.count')) || 3
    setDraft({ heartbeatInterval: hb, timeoutMultiplier: mult })
    setEditOpen(true)
  }

  const save = async () => {
    try {
      await api.updateHeartbeatConfig({
        heartbeatInterval: draft.heartbeatInterval,
        timeoutMultiplier: draft.timeoutMultiplier,
      })
      notify('心跳配置已更新')
      setEditOpen(false)
      load()
    } catch (err) {
      notify((err as { message?: string })?.message || '保存失败')
    }
  }

  const reload = async () => {
    try {
      await api.reloadConfig()
      notify('系统配置已重载')
      load()
    } catch (err) {
      notify((err as { message?: string })?.message || '重载失败')
    }
  }

  if (loading) return <Loading text="加载系统配置…" />

  if (configs.length === 0) {
    return (
      <div className="card">
        <EmptyState icon="config" title="暂无配置项" />
      </div>
    )
  }

  const rows = [
    { label: '心跳间隔', value: getValue('heartbeat.interval'), mono: true, unit: '秒' },
    { label: '超时倍数', value: getValue('heartbeat.timeout.count'), mono: true, unit: '×' },
    { label: '归档天数', value: getValue('archive.after.days'), mono: true, unit: '天' },
    { label: '历史删除天数', value: getValue('history.delete.after.days'), mono: true, unit: '天' },
    { label: '签名盐值', value: getValue('signature.salt'), mono: true },
    { label: '密钥算法', value: getValue('signature.algorithm'), mono: true },
  ]

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>系统配置</h2>
          <div className="sub">心跳、归档与签名等系统参数</div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button className="btn btn-ghost btn-sm" onClick={reload}>
            <Icon name="refresh" size={14} />
            重载配置
          </button>
          <button className="btn btn-primary btn-sm" onClick={startEdit}>
            <Icon name="config" size={14} />
            编辑心跳配置
          </button>
        </div>
      </div>

      <div className="card" style={{ padding: 18 }}>
        <table className="table">
          <thead>
            <tr>
              <th>配置项</th>
              <th>值</th>
              <th>说明</th>
            </tr>
          </thead>
          <tbody>
            {configs.map((c) => (
              <tr key={c.id}>
                <td className="mono" style={{ fontSize: 12.5 }}>
                  {c.configKey}
                </td>
                <td className="mono" style={{ fontSize: 12.5 }}>
                  {c.configValue}
                </td>
                <td style={{ fontSize: 12.5 }}>{c.configDesc}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {editOpen && (
        <Modal
          title="编辑心跳配置"
          onClose={() => setEditOpen(false)}
          width={460}
          footer={
            <>
              <button className="btn btn-ghost" onClick={() => setEditOpen(false)}>
                取消
              </button>
              <button className="btn btn-primary" onClick={save}>
                保存
              </button>
            </>
          }
        >
          <div style={{ marginBottom: 14 }}>
            <label className="field-label">心跳间隔（秒，1~86400）</label>
            <input
              className="input"
              type="number"
              min={1}
              max={86400}
              value={draft.heartbeatInterval}
              onChange={(e) => setDraft({ ...draft, heartbeatInterval: Number(e.target.value) })}
            />
          </div>
          <div>
            <label className="field-label">超时倍数（1~10）</label>
            <input
              className="input"
              type="number"
              min={1}
              max={10}
              value={draft.timeoutMultiplier}
              onChange={(e) => setDraft({ ...draft, timeoutMultiplier: Number(e.target.value) })}
            />
          </div>
        </Modal>
      )}
    </div>
  )
}