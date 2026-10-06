import React, { useEffect, useState } from 'react'
import { Icon } from '../components/Icon'
import { DetailItem, EmptyState, Loading, Modal, StatusPill } from '../components/ui'
import { api } from '../services/api'
import { License, LicenseSummary, LicenseVerifyResponse } from '../types'

interface LicensePageProps {
  notify: (msg: string) => void
}

/** 授权管理（IAS_AUTH_LIST / IMPORT / DETAIL / VERIFY / DELETE / DISABLE） */
export const LicensePage: React.FC<LicensePageProps> = ({ notify }) => {
  const [licenses, setLicenses] = useState<LicenseSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [detail, setDetail] = useState<License | null>(null)
  const [importOpen, setImportOpen] = useState(false)
  const [verify, setVerify] = useState<LicenseVerifyResponse | null>(null)

  const load = async () => {
    setLoading(true)
    try {
      const list = await api.listLicenses()
      setLicenses(list)
    } catch (err) {
      notify((err as { message?: string })?.message || '加载授权列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const filtered = licenses.filter((l) => {
    const q = search.trim().toLowerCase()
    if (!q) return true
    return (
      (l.serial || '').toLowerCase().includes(q) ||
      (l.licenseName || '').toLowerCase().includes(q) ||
      (l.licensee || '').toLowerCase().includes(q)
    )
  })

  const openDetail = async (id: number) => {
    try {
      const d = await api.getLicense(id)
      setDetail(d)
    } catch (err) {
      notify((err as { message?: string })?.message || '加载授权详情失败')
    }
  }

  const doDisable = async (lic: LicenseSummary) => {
    if (!window.confirm(`确认禁用授权 ${lic.serial}？关联实例将停止心跳。`)) return
    try {
      await api.disableLicense(lic.id)
      notify(`授权 ${lic.serial} 已禁用`)
      load()
    } catch (err) {
      notify((err as { message?: string })?.message || '禁用失败')
    }
  }

  const doDelete = async (lic: LicenseSummary) => {
    if (!window.confirm(`确认删除授权 ${lic.serial}？此操作不可恢复。`)) return
    try {
      await api.deleteLicense(lic.id)
      notify(`授权 ${lic.serial} 已删除`)
      load()
    } catch (err) {
      notify((err as { message?: string })?.message || '删除失败')
    }
  }

  const doVerify = async (id: number) => {
    try {
      const v = await api.verifyLicense(id)
      setVerify(v)
    } catch (err) {
      notify((err as { message?: string })?.message || '防篡改校验失败')
    }
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>授权管理</h2>
          <div className="sub">导入、查看、禁用与删除授权</div>
        </div>
        <button className="btn btn-primary" onClick={() => setImportOpen(true)}>
          <Icon name="upload" size={15} />
          导入授权
        </button>
      </div>

      <div className="toolbar">
        <div className="search">
          <input
            className="input"
            placeholder="搜索序列号 / 名称 / 被授权方…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <button className="btn btn-ghost btn-sm" onClick={load}>
          <Icon name="refresh" size={14} />
          刷新
        </button>
      </div>

      {loading ? (
        <Loading />
      ) : filtered.length === 0 ? (
        <div className="card">
          <EmptyState icon="license" title="暂无授权" desc="点击右上角「导入授权」添加授权文件" />
        </div>
      ) : (
        <div className="card" style={{ overflow: 'auto' }}>
          <table className="table">
            <thead>
              <tr>
                <th>序列号</th>
                <th>名称</th>
                <th>产品</th>
                <th>模式</th>
                <th>配额</th>
                <th>状态</th>
                <th>到期</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((l) => {
                const pct = l.maxInstances > 0 ? Math.round((l.usedInstances / l.maxInstances) * 100) : 0
                return (
                  <tr key={l.id}>
                    <td className="mono" style={{ fontSize: 12.5 }}>
                      {l.serial}
                    </td>
                    <td>{l.licenseName || '—'}</td>
                    <td>
                      {l.proname}
                      <div style={{ fontSize: 11.5, color: 'var(--muted)' }}>{l.version}</div>
                    </td>
                    <td>{l.licenseMode}</td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        <div className="bar-track" style={{ width: 60 }}>
                          <div className="bar-fill" style={{ width: `${Math.min(100, pct)}%` }} />
                        </div>
                        <span className="mono" style={{ fontSize: 12 }}>
                          {l.usedInstances}/{l.maxInstances}
                        </span>
                      </div>
                    </td>
                    <td>
                      <StatusPill status={l.status} />
                    </td>
                    <td className="mono" style={{ fontSize: 12 }}>
                      {l.expiration === 'never' ? '永久' : l.expiration}
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: 6 }}>
                        <button className="btn btn-ghost btn-sm" onClick={() => openDetail(l.id)}>
                          详情
                        </button>
                        <button className="btn btn-ghost btn-sm" onClick={() => doVerify(l.id)}>
                          校验
                        </button>
                        {l.status !== 'DISABLED' && (
                          <button className="btn btn-ghost btn-sm" onClick={() => doDisable(l)}>
                            禁用
                          </button>
                        )}
                        <button className="btn btn-danger btn-sm" onClick={() => doDelete(l)}>
                          删除
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      {detail && (
        <Modal title={`授权详情 · ${detail.serial}`} onClose={() => setDetail(null)} width={680}>
          <div className="detail-grid">
            <DetailItem k="序列号" v={detail.serial} mono />
            <DetailItem k="名称" v={detail.licenseName} />
            <DetailItem k="产品" v={`${detail.proname} ${detail.version}`} />
            <DetailItem k="组件" v={detail.component} />
            <DetailItem k="被授权方" v={detail.licensee} />
            <DetailItem k="模式" v={detail.licenseMode} />
            <DetailItem k="正式" v={detail.formal} />
            <DetailItem k="到期" v={detail.expiration === 'never' ? '永久' : detail.expiration} />
            <DetailItem k="状态" v={<StatusPill status={detail.status} />} />
            <DetailItem k="来源" v={detail.source} />
            <DetailItem k="实例配额" v={`${detail.usedInstances}/${detail.maxInstances}`} mono />
            <DetailItem k="剩余实例" v={detail.remainingInstances} mono />
            <DetailItem k="CPU" v={detail.maxCpus ?? '—'} mono />
            <DetailItem k="内存(MB)" v={detail.maxMemory ?? '—'} mono />
            <DetailItem k="用户信息" v={detail.userinfor} />
          </div>
          {detail.bxbFile && (
            <div style={{ marginTop: 16 }}>
              <div className="field-label">授权文件（XML）</div>
              <textarea
                className="textarea"
                rows={6}
                readOnly
                value={detail.bxbFile}
                style={{ fontFamily: 'var(--font-mono)', fontSize: 11.5 }}
              />
            </div>
          )}
        </Modal>
      )}

      {verify && (
        <Modal title={`防篡改校验 · ${verify.serial}`} onClose={() => setVerify(null)} width={520}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 16 }}>
            <StatusPill status={verify.verified ? 'success' : 'danger'} />
            <span style={{ fontSize: 13 }}>{verify.message}</span>
          </div>
          <table className="table">
            <thead>
              <tr>
                <th>校验层</th>
                <th>结果</th>
                <th>明细</th>
              </tr>
            </thead>
            <tbody>
              {verify.layers.map((layer, i) => (
                <tr key={i}>
                  <td className="mono" style={{ fontSize: 12 }}>
                    {layer.name}
                  </td>
                  <td>
                    <StatusPill status={layer.passed ? 'success' : 'danger'} />
                  </td>
                  <td style={{ fontSize: 12.5 }}>{layer.detail}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Modal>
      )}

      {importOpen && <ImportModal onClose={() => setImportOpen(false)} onImported={() => { notify('授权导入成功'); load() }} notify={notify} />}
    </div>
  )
}

/** 授权导入弹窗（IAS_AUTH_IMPORT） */
const ImportModal: React.FC<{
  onClose: () => void
  onImported: () => void
  notify: (msg: string) => void
}> = ({ onClose, onImported, notify }) => {
  const [licenseFile, setLicenseFile] = useState('')
  const [licenseName, setLicenseName] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const submit = async () => {
    if (!licenseFile.trim()) {
      setError('请粘贴授权文件 XML 内容')
      return
    }
    setLoading(true)
    setError(null)
    try {
      await api.importLicense({ licenseFile, licenseName: licenseName || undefined })
      onImported()
      onClose()
    } catch (err) {
      setError((err as { message?: string })?.message || '导入失败')
      setLoading(false)
    }
  }

  return (
    <Modal
      title="导入授权"
      onClose={onClose}
      width={620}
      footer={
        <>
          <button className="btn btn-ghost" onClick={onClose}>
            取消
          </button>
          <button className="btn btn-primary" onClick={submit} disabled={loading}>
            {loading ? '导入中…' : '导入'}
          </button>
        </>
      }
    >
      <div style={{ marginBottom: 14 }}>
        <label className="field-label">授权名称（可选）</label>
        <input
          className="input"
          value={licenseName}
          onChange={(e) => setLicenseName(e.target.value)}
          placeholder="便于管理台识别，如：生产环境企业版"
        />
      </div>
      <div>
        <label className="field-label">授权文件 XML 内容</label>
        <textarea
          className="textarea"
          rows={10}
          value={licenseFile}
          onChange={(e) => {
            setLicenseFile(e.target.value)
            if (error) setError(null)
          }}
          placeholder="粘贴 license.infor 授权文件原始 XML 文本…"
          style={{ fontFamily: 'var(--font-mono)', fontSize: 11.5 }}
        />
      </div>
      {error && (
        <div className="login-error" role="alert" style={{ marginTop: 12 }}>
          <Icon name="alert" size={15} style={{ flex: 'none', marginTop: 1 }} />
          <span>{error}</span>
        </div>
      )}
    </Modal>
  )
}