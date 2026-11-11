import React, { useState } from 'react';
import type { User } from '../../types';

export function AvatarUpload({ user, disabled, onSelect }: { user: User; disabled: boolean; onSelect: (file: File) => void }) {
    const [error, setError] = useState('');
    return <div className="user-avatar-upload">
        {user.avatarUrl ? <img className="user-avatar-preview" src={user.avatarUrl} alt="当前头像"/> : <span className="user-avatar-preview user-avatar-empty" aria-label="尚未上传头像">{user.nickname.slice(0, 1)}</span>}
        <label>上传头像<input name="avatarFile" type="file" accept="image/jpeg,image/png" disabled={disabled} onChange={event => {
            const file = event.currentTarget.files?.[0]; event.currentTarget.value = ''; setError('');
            if (!file) return;
            if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 1024 * 1024) { setError('请选择不超过1MB的 JPG 或 PNG 图片'); return; }
            onSelect(file);
        }}/></label>
        <p className="fine-print">JPG / PNG，最大1MB。自动截取图片中央并裁为圆形，选择后立即保存。</p>
        {error && <p role="alert">{error}</p>}
    </div>;
}
