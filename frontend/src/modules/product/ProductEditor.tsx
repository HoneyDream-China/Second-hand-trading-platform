import React, { useEffect, useState } from 'react';
import type { Product } from '../../types';
import { categories } from '../../shared/ui';

type ImageSelection = string | File;
function ImagePreview({ image, index }: { image: ImageSelection; index: number }) {
    const [url, setUrl] = useState(typeof image === 'string' ? image : '');
    useEffect(() => {
        if (typeof image === 'string') { setUrl(image); return; }
        const preview = URL.createObjectURL(image);
        setUrl(preview);
        return () => URL.revokeObjectURL(preview);
    }, [image]);
    return <img src={url || undefined} alt={`商品图片 ${index + 1}`}/>;
}
function ProductImagePicker({ images, onChange, disabled }: { images: ImageSelection[]; onChange: (images: ImageSelection[]) => void; disabled: boolean }) {
    const [error, setError] = useState('');
    return <div className="product-image-picker">
        <label htmlFor="product-images">商品图片（{images.length}/9）</label>
        <p className="field-hint">最多上传 9 张，第一张作为探索好物的浏览图。支持 JPG / PNG，每张不超过 5MB。</p>
        <div className="product-image-grid">{images.map((image, index) => <div className="product-image-item" key={typeof image === 'string' ? `${index}-${image}` : `${index}-${image.name}-${image.lastModified}`}>
            <ImagePreview image={image} index={index}/>
            <span className="product-image-badge">{index === 0 ? '封面' : index + 1}</span>
            <button type="button" disabled={disabled} aria-label={`移除第 ${index + 1} 张图片`} onClick={() => { onChange(images.filter((_, i) => i !== index)); setError(''); }}>移除</button>
        </div>)}</div>
        <input id="product-images" type="file" multiple accept="image/jpeg,image/png" disabled={disabled || images.length >= 9} onChange={event => {
            const files = Array.from(event.currentTarget.files || []);
            event.currentTarget.value = '';
            setError('');
            if (images.length + files.length > 9) { setError('商品图片最多上传 9 张，请重新选择'); return; }
            if (files.some(file => !['image/jpeg', 'image/png'].includes(file.type) || file.size === 0 || file.size > 5 * 1024 * 1024)) { setError('请选择不超过 5MB 的 JPG 或 PNG 图片'); return; }
            onChange([...images, ...files]);
        }}/>
        {error && <p role="alert" className="product-image-error">{error}</p>}
    </div>;
}
export function ProductEditor({ existing, loading, onSubmit }: { existing?: Product; loading: boolean; onSubmit: (event: React.FormEvent<HTMLFormElement>, images: ImageSelection[]) => void }) {
    const [images, setImages] = useState<ImageSelection[]>(existing?.imageUrls || []);
    return <form className="editor-form" onSubmit={e => onSubmit(e, images)}><fieldset className="product-editor-fields" disabled={loading}><label>商品名称<input name="title" required maxLength={120} defaultValue={existing?.title} placeholder="给你的好物起个名字"/></label><div className="form-grid"><label>价格 / RMB<input name="price" type="number" min="0" step="0.01" max="9999999999.99" required defaultValue={existing?.price} placeholder="0.00"/></label><label>商品分类<select name="category" defaultValue={existing?.category}>{categories.slice(1).map(c => <option key={c}>{c}</option>)}</select></label><label>商品成色<select name="condition" defaultValue={existing?.condition}>{['全新', '九五新', '九成新', '八成新'].map(c => <option key={c}>{c}</option>)}</select></label></div><label>商品描述<textarea name="description" required maxLength={5000} rows={6} defaultValue={existing?.description} placeholder="使用情况、购买时间、配件，以及你想告诉下一位主人的事。"/></label><ProductImagePicker images={images} onChange={setImages} disabled={loading}/><div className="form-actions"><a href="#/manage" className="text-link">取消</a><button className="button" disabled={loading}>{existing ? '保存修改' : '保存为草稿'}</button></div><p className="fine-print">发布后初始状态为草稿。你可以在“我的发布”中确认信息并上架。</p></fieldset></form>;
}
