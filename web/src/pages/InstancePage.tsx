import React, { useEffect, useState } from 'react'
import { Icon } from '../components/Icon'
import { DetailItem, EmptyState, Loading, Modal, StatusPill } from '../components/ui'
import { api } from '../services/api'
import { HistoryInstance, Instance } from '../types'

interface InstancePageProps {
  notify: (msg: string) => void
}

type Tab = 'online' | 'offline' | 'history'

/** 实例监控（IAS_AUTH_INST_LIST / OFFLINE / HISTORY / DETAIL） */
export const InstancePage: React.FC<InstancePageProps> = ({ notify }) => {
  const [tab, setTab] = useState<Tab>('online')
  const [online, setOnline] = useState<Instance[]>([])
  const [offline, setOffline] = useState<Instance[]>([])
  const [history, setHistory] = useState<HistoryInstance[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [detail, setDetail] = useState<Instance | null>(null)

  const load = async () => {
    setLoading(true)
    try {
      const [on, off, his] = await Promise.all([
        api.listOnlineInstances(),
        api.listOfflineInstances(),
        api.listHistoryInstances(),
      ])
      setOnline(on)
      setOffline(off)
      setHistory(his)
    } catch (err) {
      notify((err as { message?: string })?.message || '加载实例列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const list = tab === 'online' ? online : tab === 'offline' ? offline : history
  const filtered = list.filter((i) => {
    const q = search.trim().toLowerCase()
    if (!q) return true
    return (
      (i.instanceId || '').toLowerCase().includes(q) ||
      (i.hostname || '').toLowerCase().includes(q) ||
      (i.ipAddress || '').toLowerCase().includes(q)
    )
  })

  const openDetail = async (id: number) => {
    try {
      const d = await api.getInstance(id)
      setDetail(d)
    } catch (err) {
      notify((err as { message?: string })?.message || '加载实例详情失败')
    }
  }

  const tabs: { id: Tab; label: string; count: number }[] = [
    { id: 'online', label: '在线实例', count: online.length },
    { id: 'offline', label: '下线实例', count: offline.length },
    { id: 'history', label: '历史归档', count: history.length },
  ]

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>实例监控</h2>
          <div className="sub">在线实例、下线实例与历史归档</div>
        </div>
        <button className="btn btn-ghost btn-sm" onClick={load}>
          <Icon name="refresh" size={14} />
          刷新
        </button>
      </div>

      <div className="tabs">
        {tabs.map((t) => (
          <div
            key={t.id}
            className={`tab ${tab === t.id ? 'active' : ''}`}
            onClick={() => setTab(t.id)}
          >
            {t.label} <span className="mono" style={{ fontSize: 12 }}>({t.count})</span>
          </div>
        ))}
      </div>

      <div className="toolbar">
        <div className="search">
          <input
            className="input"
            placeholder="搜索实例 ID / 主机名 / IP…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      {loading ? (
        <Loading />
      ) : filtered.length === 0 ? (
        <div className="card">
          <EmptyState icon="instance" title="暂无实例" desc="当前分类下没有实例记录" />
        </div>
      ) : (
        <div className="card" style={{ overflow: 'auto' }}>
          <table className="table">
            <thead>
              <tr>
                <th>实例 ID</th>
                <th>主机名</th>
                <th>IP</th>
                <th>产品</th>
                <th>状态</th>
                <th>最近心跳</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((i) => (
                <tr key={i.id}>
                  <td className="mono" style={{ fontSize: 12 }}>
                    {i.instanceId}
                  </td>
                  <td>{i.hostname}</td>
                  <td className="mono" style={{ fontSize: 12 }}>
                    {i.ipAddress}
                  </td>
                  <td>
                    {i.productType}
                    <div style={{ fontSize: 11.5, color: 'var(--muted)' }}>{i.productVersion}</div>
                  </td>
                  <td>
                    <StatusPill status={i.status} />
                  </td>
                  <td className="mono" style={{ fontSize: 12 }}>
                    {i.lastHeartbeatTime ? new Date(i.lastHeartbeatTime).toLocaleString() : '—'}
                  </td>
                  <td>
                    <button className="btn btn-ghost btn-sm" onClick={() => openDetail(i.id)}>
                      详情
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {detail && (
        <Modal title={`实例详情 · ${detail.instanceId}`} onClose={() => setDetail(null)} width={680}>
          <div className="detail-grid">
            <DetailItem k="实例 ID" v={detail.instanceId} mono />
            <DetailItem k="授权 ID" v={detail.licenseId} mono />
            <DetailItem k="客户端 UUID" v={detail.clientUuid} mono />
            <DetailItem k="产品" v={`${detail.productType} ${detail.productVersion}`} />
            <DetailItem k="规格" v={detail.productSpec} />
            <DetailItem k="主机名" v={detail.hostname} />
            <DetailItem k="IP" v={detail.ipAddress} mono />
            <DetailItem k="MAC" v={detail.mac} mono />
            <DetailItem k="机器类型" v={detail.machineType} />
            <DetailItem k="状态" v={<StatusPill status={detail.status} />} />
            <DetailItem k="CPU" v={detail.currentCpus} mono />
            <DetailItem k="内存(MB)" v={detail.currentMemory} mono />
            <DetailItem k="上线时间" v={detail.onlineTime ? new Date(detail.onlineTime).toLocaleString() : '—'} />
            <DetailItem k="最近心跳" v={detail.lastHeartbeatTime ? new Date(detail.lastHeartbeatTime).toLocaleString() : '—'} />
          </div>
        </Modal>
      )}
    </div>
  )
}