import type { Product } from '../types';
import Art from '../Art';
export function Icon({ name }: {
    name: 'search' | 'user' | 'loop' | 'arrow';
}) { return <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">{name === 'search' ? <><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></> : name === 'user' ? <><circle cx="12" cy="8" r="3.5"/><path d="M5 21v-2a7 7 0 0 1 14 0v2"/></> : name === 'arrow' ? <path d="m9 5 7 7-7 7"/> : <><path d="M19 9a7.5 7.5 0 0 0-13-3L3 9m0-5v5h5M5 15a7.5 7.5 0 0 0 13 3l3-3m0 5v-5h-5"/></>}</svg>; }
export const image = (item: Product, className = '') => item.imageUrls?.[0] ? <img className={`uploaded-art ${className}`} src={item.imageUrls[0]} alt={item.title} onError={e => { e.currentTarget.style.visibility = 'hidden'; }}/> : <Art kind={item.art || 'books'} className={className}/>;
export const intro = (eyebrow: string, title: string, subtitle: string) => <div className="page-intro"><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{subtitle}</p></div>;
export const formValue = (data: FormData, name: string) => String(data.get(name) || '').trim();
export const categories = ['全部好物', '数码设备', '图书教材', '生活好物', '运动户外'];
export const money = (value: string) => Number(value).toLocaleString('zh-CN', { maximumFractionDigits: 2 });
export const now = () => new Date().toISOString();
