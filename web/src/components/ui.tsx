import React, { useEffect } from 'react'
import { Icon } from './Icon'

/** 状态徽标 */
export const StatusPill: React.FC<{ status: string; label?: string }> = ({ status, label }) => {
  const map: Record<string, [string, string]> = {
    online: ['success', '在线'],
    offline: ['neutral', '离线'],
    archived: ['neutral', '已归档'],
    active: ['success', '启用'],
    disabled: ['neutral', '已禁用'],
    expired: ['danger', '已过期'],
    success: ['success', '成功'],
    warn: ['warn', '警告'],
    danger: ['danger', '失败'],
    ACTIVE: ['success', '启用'],
    DISABLED: ['neutral', '已禁用'],
    EXPIRED: ['danger', '已过期'],
    ONLINE: ['success', '在线'],
    OFFLINE: ['neutral', '离线'],
  }
  const [cls, text] = map[status] || ['neutral', label || status]
  return (
    <span className={`pill pill-${cls}`}>
      <span className="dot" />
      {text}
    </span>
  )
}

/** 轻提示 */
export const Toast: React.FC<{ msg: string; onClose: () => void }> = ({ msg, onClose }) => {
  useEffect(() => {
    const t = setTimeout(onClose, 2600)
    return () => clearTimeout(t)
  }, [msg, onClose])
  return (
    <div className="toast fade-in">
      <Icon name="check" size={16} />
      {msg}
    </div>
  )
}

/** 模态框 */
export const Modal: React.FC<{
  title: string
  onClose: () => void
  children: React.ReactNode
  width?: number
  footer?: React.ReactNode
}> = ({ title, onClose, children, width = 560, footer }) => (
  <div className="modal-overlay" onClick={onClose}>
    <div
      className="modal-card fade-in"
      style={{ width, maxWidth: '92vw' }}
      onClick={(e) => e.stopPropagation()}
    >
      <div className="modal-head">
        <h3>{title}</h3>
        <button className="btn btn-ghost btn-sm" onClick={onClose} aria-label="关闭">
          <Icon name="close" size={16} />
        </button>
      </div>
      <div className="modal-body">{children}</div>
      {footer && <div className="modal-foot">{footer}</div>}
    </div>
  </div>
)

/** 空状态 */
export const EmptyState: React.FC<{ icon: string; title: string; desc?: string }> = ({
  icon,
  title,
  desc,
}) => (
  <div className="empty-state">
    <Icon name={icon} size={40} />
    <div style={{ fontWeight: 600, marginTop: 8 }}>{title}</div>
    {desc && <div style={{ fontSize: 13, marginTop: 4 }}>{desc}</div>}
  </div>
)

/** 加载占位 */
export const Loading: React.FC<{ text?: string }> = ({ text = '加载中…' }) => (
  <div className="empty-state">
    <span className="spin" />
    <div style={{ fontSize: 13, marginTop: 10 }}>{text}</div>
  </div>
)

/** 详情字段 */
export const DetailItem: React.FC<{ k: string; v: React.ReactNode; mono?: boolean }> = ({
  k,
  v,
  mono,
}) => (
  <div className="detail-item">
    <div className="k">{k}</div>
    <div className={`v ${mono ? 'mono' : ''}`}>{v || '—'}</div>
  </div>
)