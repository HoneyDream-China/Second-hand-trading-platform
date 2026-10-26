/** 成员 C：交易申请、买卖列表、状态操作和状态记录。 */
import React, { useState } from 'react';
import { tradeApi } from './api';
import { DEMO } from '../../shared/request';
import { useApp } from '../../shared/AppContext';
import { intro, image, money, now, formValue } from '../../shared/ui';
import { tradeLabels, type Trade, type Product } from '../../types';
import './trade.css';
export function TradeRequest({ product }: {
    product: Product;
}) {
    const { route, user, trades, setTrades, loading, go, guard, run, notify } = useApp();
    return (<form onSubmit={e => { e.preventDefault(); if (!user) {
        guard(route);
        return;
    } const data = new FormData(e.currentTarget); void run(async () => { if (trades.some(t => t.productId === product.id && t.status === 'PENDING'))
        throw new Error('此商品已有待处理交易，请等待处理'); const message = formValue(data, 'message'); const trade: Trade = DEMO ? { id: Date.now(), productId: product.id, buyerId: user.id, sellerId: product.sellerId, message, status: 'PENDING', createdAt: now(), logs: [{ status: 'PENDING', at: now() }] } : await tradeApi.createTrade({ productId: product.id, message }); setTrades(list => [trade, ...list]); go('/trades'); notify('交易申请已提交，等待卖家确认'); }); }}><label>给卖家留言 <span className="muted">（选填）</span><textarea name="message" placeholder="聊聊你希望的交接时间和地点" maxLength={500}/></label><button className="button full" disabled={loading}>申请交易</button></form>);
}
export default function TradeSubsystem() {
    const { user, products, setProducts, trades, setTrades, loading, run, notify } = useApp();
    const [tradeFilter, setTradeFilter] = useState('ALL');
    const [role, setRole] = useState('ALL');
    const transition = (trade: Trade, action: 'accept' | 'reject' | 'cancel') => void run(async () => {
        const item = products.find(p => p.id === trade.productId);
        if (trade.status !== 'PENDING')
            throw new Error('此交易已处理');
        if (action === 'accept' && item?.status !== 'ON_SALE')
            throw new Error('商品当前不可交易');
        const status = action === 'accept' ? 'ACCEPTED' : action === 'reject' ? 'REJECTED' : 'CANCELLED';
        const updated: Trade = DEMO ? { ...trade, status, logs: [...trade.logs, { status, at: now() }] } : await tradeApi.transition(trade.id, action);
        setTrades(list => list.map(t => t.id === trade.id ? updated : t));
        if (action === 'accept' && item)
            setProducts(list => list.map(p => p.id === item.id ? { ...p, status: 'SOLD', version: p.version + 1 } : p));
        notify(`交易${tradeLabels[status]}`);
    });
    if (!user)
        return null;
    let content: React.ReactNode;
    const list = trades.filter(t => (t.buyerId === user.id || t.sellerId === user.id) && (tradeFilter === 'ALL' || t.status === tradeFilter) && (role === 'ALL' || (role === 'BUY' ? t.buyerId === user.id : t.sellerId === user.id)));
    content = <main className="wrap">{intro('交易中心', '每一次心动，都有回应。', '查看买入与卖出的申请，跟进每一步交易。')}<div className="section-toolbar"><div className="category-tabs">{['ALL', 'PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED'].map(s => <button key={s} className={tradeFilter === s ? 'selected' : ''} onClick={() => setTradeFilter(s)}>{s === 'ALL' ? '全部交易' : tradeLabels[s as Trade['status']]}</button>)}</div><select aria-label="交易角色" value={role} onChange={e => setRole(e.target.value)}><option value="ALL">买入与卖出</option><option value="BUY">我买入的</option><option value="SELL">我卖出的</option></select></div>{list.map(t => { const item = products.find(p => p.id === t.productId); return <article className="trade-row" key={t.id}><div className="trade-heading"><span>{t.buyerId === user.id ? '我买入的' : '我卖出的'} · 交易编号 {t.id}</span><span className="status">{tradeLabels[t.status]}</span></div><div className="trade-body"><a className="mini-art" href={`#/product/${t.productId}`}>{item && image(item)}</a><div><h3>{item?.title || `商品 ${t.productId}`}</h3><p>{item ? `RMB ${money(item.price)}` : ''}</p><p className="muted">{t.message || '暂无留言'}</p></div><div className="row-controls">{t.status === 'PENDING' && (t.sellerId === user.id ? <><button className="button" disabled={loading} onClick={() => transition(t, 'accept')}>接受申请</button><button className="text-link" disabled={loading} onClick={() => transition(t, 'reject')}>拒绝</button></> : <button className="text-link" disabled={loading} onClick={() => transition(t, 'cancel')}>取消申请</button>)}</div></div><details><summary>查看状态记录</summary><ol className="timeline">{(t.logs || [{ status: t.status, at: t.createdAt }]).map((log, i) => <li key={i}><span>{tradeLabels[log.status]}</span><time>{new Date(log.at).toLocaleString('zh-CN')}</time></li>)}</ol></details></article>; })}{!list.length && <div className="empty"><h2>这里，等待下一次心动。</h2><p>目前没有符合条件的交易。</p><a href="#/explore" className="text-link">去探索好物 ›</a></div>}</main>;
    return content;
}
