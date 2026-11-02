import React, { useState } from 'react';
import type { User } from '../../types';

export async function circularPreview(file: File): Promise<string> {
    const bitmap = await createImageBitmap(file);
    try {
        if (bitmap.width * bitmap.height > 16_000_000) throw new Error('图片不能超过1600万像素');
        const canvas = document.createElement('canvas');
        canvas.width = canvas.height = 256;
        const context = canvas.getContext('2d');
        if (!context) throw new Error('浏览器无法处理图片');
        const side = Math.min(bitmap.width, bitmap.height);
        context.beginPath(); context.arc(128, 128, 128, 0, Math.PI * 2); context.clip();
        context.drawImage(bitmap, (bitmap.width - side) / 2, (bitmap.height - side) / 2, side, side, 0, 0, 256, 256);
        return canvas.toDataURL('image/png');
    } finally { bitmap.close(); }
}

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
