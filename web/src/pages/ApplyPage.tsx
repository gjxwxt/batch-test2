import React, { useState } from 'react'
import { Icon } from '../components/Icon'
import { api } from '../services/api'

interface ApplyPageProps {
  notify: (msg: string) => void
}

/** 授权文件申请（IAS_AUTH_FILE_APPLY，local/site 模式） */
export const ApplyPage: React.FC<ApplyPageProps> = ({ notify }) => {
  const [mode, setMode] = useState('local')
  const [product, setProduct] = useState('InforSuite AS')
  const [edition, setEdition] = useState('企业版')
  const [licensee, setLicensee] = useState('')
  const [maxInstances, setMaxInstances] = useState(10)
  const [expireAt, setExpireAt] = useState('')
  const [remark, setRemark] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!licensee.trim()) {
      setError('请填写被授权方')
      return
    }
    setLoading(true)
    setError(null)
    try {
      const res = await api.fileApply({
        mode,
        product,
        edition,
        licensee: licensee.trim(),
        maxInstances,
        expireAt: expireAt || undefined,
        remark: remark || undefined,
      })
      notify(`${res.message || '授权文件申请已提交'}（${res.applyId}）`)
      setLicensee('')
      setRemark('')
    } catch (err) {
      setError((err as { message?: string })?.message || '申请失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div>
      <div className="page-head">
        <div>
          <h2>授权文件申请</h2>
          <div className="sub">申请 local / site 模式授权文件</div>
        </div>
      </div>

      <div className="card" style={{ padding: 24, maxWidth: 640 }}>
        <form onSubmit={submit} noValidate>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
            <div>
              <label className="field-label">授权模式</label>
              <select className="select" value={mode} onChange={(e) => setMode(e.target.value)}>
                <option value="local">local（单机）</option>
                <option value="site">site（站点）</option>
              </select>
            </div>
            <div>
              <label className="field-label">产品</label>
              <input className="input" value={product} onChange={(e) => setProduct(e.target.value)} />
            </div>
            <div>
              <label className="field-label">版本</label>
              <input className="input" value={edition} onChange={(e) => setEdition(e.target.value)} />
            </div>
            <div>
              <label className="field-label">被授权方 *</label>
              <input
                className="input"
                value={licensee}
                onChange={(e) => {
                  setLicensee(e.target.value)
                  if (error) setError(null)
                }}
                placeholder="请输入被授权方名称"
              />
            </div>
            <div>
              <label className="field-label">实例配额</label>
              <input
                className="input"
                type="number"
                min={1}
                value={maxInstances}
                onChange={(e) => setMaxInstances(Number(e.target.value))}
              />
            </div>
            <div>
              <label className="field-label">到期日期（可选）</label>
              <input className="input" type="date" value={expireAt} onChange={(e) => setExpireAt(e.target.value)} />
            </div>
          </div>
          <div style={{ marginTop: 14 }}>
            <label className="field-label">备注（可选）</label>
            <textarea
              className="textarea"
              rows={3}
              value={remark}
              onChange={(e) => setRemark(e.target.value)}
              placeholder="申请用途说明…"
            />
          </div>
          {error && (
            <div className="login-error" role="alert" style={{ marginTop: 12 }}>
              <Icon name="alert" size={15} style={{ flex: 'none', marginTop: 1 }} />
              <span>{error}</span>
            </div>
          )}
          <button className="btn btn-primary" type="submit" disabled={loading} style={{ marginTop: 18 }}>
            {loading ? '提交中…' : '提交申请'}
          </button>
        </form>
      </div>
    </div>
  )
}