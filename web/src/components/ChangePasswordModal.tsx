import React, { useState } from 'react'
import { Icon } from './Icon'
import { Modal } from './ui'
import { api } from '../services/api'

interface ChangePasswordModalProps {
  onClose: () => void
  notify: (msg: string) => void
}

/** 修改密码弹窗（IAS_AUTH_CHANGE_PWD） */
export const ChangePasswordModal: React.FC<ChangePasswordModalProps> = ({ onClose, notify }) => {
  const [oldPassword, setOldPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [show, setShow] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const submit = async () => {
    if (!oldPassword || !newPassword) {
      setError('请填写旧密码与新密码')
      return
    }
    if (newPassword !== confirm) {
      setError('两次输入的新密码不一致')
      return
    }
    setLoading(true)
    setError(null)
    try {
      await api.changePassword({ oldPassword, newPassword })
      notify('管理员密码已修改，并记录审计事件')
      onClose()
    } catch (err) {
      setError((err as { message?: string })?.message || '修改密码失败')
      setLoading(false)
    }
  }

  const field = (label: string, value: string, setter: (v: string) => void, ph: string) => (
    <div style={{ marginBottom: 14 }}>
      <label className="field-label">{label}</label>
      <div className="pwd-wrap">
        <input
          className="input"
          type={show ? 'text' : 'password'}
          value={value}
          onChange={(e) => {
            setter(e.target.value)
            if (error) setError(null)
          }}
          placeholder={ph}
        />
        <button type="button" className="pwd-toggle" onClick={() => setShow((v) => !v)} aria-label={show ? '隐藏密码' : '显示密码'}>
          <Icon name={show ? 'eye-off' : 'eye'} size={16} />
        </button>
      </div>
    </div>
  )

  return (
    <Modal
      title="修改密码"
      onClose={onClose}
      width={440}
      footer={
        <>
          <button className="btn btn-ghost" onClick={onClose}>
            取消
          </button>
          <button className="btn btn-primary" onClick={submit} disabled={loading}>
            {loading ? '提交中…' : '确认修改'}
          </button>
        </>
      }
    >
      {field('旧密码', oldPassword, setOldPassword, '请输入旧密码')}
      {field('新密码', newPassword, setNewPassword, '请输入新密码')}
      {field('确认新密码', confirm, setConfirm, '再次输入新密码')}
      {error && (
        <div className="login-error" role="alert">
          <Icon name="alert" size={15} style={{ flex: 'none', marginTop: 1 }} />
          <span>{error}</span>
        </div>
      )}
    </Modal>
  )
}