/** 成员 B：商品探索、详情、发布、编辑和我的商品。 */
import React, { useState } from 'react';
import { productApi } from './api';
import { useApp } from '../../shared/AppContext';
import { Icon, intro, image, money, categories, formValue } from '../../shared/ui';
import { productLabels, type Product, type ProductStatus } from '../../types';
import { TradeRequest } from '../trade/TradeSubsystem';
import './product.css';
import { ProductEditor } from './ProductEditor';
export default function ProductSubsystem() {
    const { route, user, products, setProducts, category, setCategory, page, setPage, loading, go, notify, run, guard } = useApp();
    const [keyword, setKeyword] = useState('');
    const [condition, setCondition] = useState('');
    const [min, setMin] = useState('');
    const [max, setMax] = useState('');
    const [sort, setSort] = useState('new');
    const detailId = Number(route.split('/')[2]);
    const product = products.find(p => p.id === detailId);
    const updateStatus = (item: Product, status: ProductStatus) => void run(async () => {
        const updated = await productApi.status(item.id, { status, version: item.version });
        setProducts(list => list.map(p => p.id === item.id ? updated : p));
        notify(`商品${productLabels[status]}`);
    });
    const submitProduct = (event: React.FormEvent<HTMLFormElement>, images: (string | File)[], existing?: Product) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        void run(async () => {
            const price = formValue(data, 'price');
            if (!/^\d{1,10}(\.\d{1,2})?$/.test(price))
                throw new Error('价格最多支持 10 位整数和 2 位小数');
            if (images.length > 9) throw new Error('商品图片最多上传 9 张');
            const imageUrls: string[] = [];
            for (const image of images) imageUrls.push(typeof image === 'string' ? image : await productApi.uploadImage(image));
            const body = { title: formValue(data, 'title'), description: formValue(data, 'description'), price: Number(price).toFixed(2), category: formValue(data, 'category'), condition: formValue(data, 'condition'), imageUrls };
            const updated: Product = existing ? await productApi.editProduct(existing.id, { ...body, version: existing.version }) : await productApi.createProduct(body);
            setProducts(list => existing ? list.map(p => p.id === existing.id ? updated : p) : [updated, ...list]);
            go('/manage');
            notify(existing ? '商品已更新' : '草稿已保存，可在我的发布中上架');
        });
    };
    let content: React.ReactNode = null;
    if (route === '/explore') {
        const filtered = products.filter(p => p.status === 'ON_SALE' && (category === '全部好物' || p.category === category) && `${p.title}${p.description}`.toLowerCase().includes(keyword.toLowerCase()) && (!condition || p.condition === condition) && (!min || Number(p.price) >= Number(min)) && (!max || Number(p.price) <= Number(max))).sort((a, b) => sort === 'low' ? Number(a.price) - Number(b.price) : sort === 'high' ? Number(b.price) - Number(a.price) : b.createdAt.localeCompare(a.createdAt));
        content = <main className="wrap">{intro('探索校园好物', '下一件心动，正在这里。', '数码、书籍、生活灵感。好物不必全新，喜欢刚刚好。')}<div className="category-tabs" role="group" aria-label="商品分类">{categories.map(c => <button className={category === c ? 'selected' : ''} key={c} onClick={() => { setCategory(c); setPage(1); }}>{c}</button>)}</div><div className="filters"><label className="search-field"><Icon name="search"/><input id="search" placeholder="搜索你的下一件好物" value={keyword} onChange={e => { setKeyword(e.target.value); setPage(1); }}/></label><select aria-label="成色" value={condition} onChange={e => { setCondition(e.target.value); setPage(1); }}><option value="">全部成色</option>{['全新', '九五新', '九成新', '八成新'].map(c => <option key={c}>{c}</option>)}</select><input aria-label="最低价格" type="number" min="0" placeholder="最低价" value={min} onChange={e => { setMin(e.target.value); setPage(1); }}/><span>—</span><input aria-label="最高价格" type="number" min="0" placeholder="最高价" value={max} onChange={e => { setMax(e.target.value); setPage(1); }}/><select aria-label="排序" value={sort} onChange={e => setSort(e.target.value)}><option value="new">最新发布</option><option value="low">价格从低到高</option><option value="high">价格从高到低</option></select></div><p className="result-count">{filtered.length} 件好物，等你发现</p><div className="product-list">{filtered.slice((page - 1) * 4, page * 4).map(p => <article className="product-row" key={p.id}><a className="row-art" href={`#/product/${p.id}`}>{image(p)}</a><div><span className="eyebrow">{p.category} / {p.condition}</span><a href={`#/product/${p.id}`}><h2>{p.title}</h2></a><p>{p.description}</p><span className="seller">由 {p.sellerName} 发布</span></div><div className="row-end"><strong>RMB {money(p.price)}</strong><a className="text-link" href={`#/product/${p.id}`}>进一步了解 <span>›</span></a></div></article>)}</div>{!filtered.length && <div className="empty"><h2>暂时没有找到。</h2><p>换个关键词，心动也许就在下一次搜索。</p><button className="text-link" onClick={() => { setKeyword(''); setCategory('全部好物'); setCondition(''); setMin(''); setMax(''); }}>清除筛选 ›</button></div>}<div className="pagination"><button disabled={page === 1} onClick={() => setPage(page - 1)}>上一页</button><span>{page} / {Math.max(1, Math.ceil(filtered.length / 4))}</span><button disabled={page * 4 >= filtered.length} onClick={() => setPage(page + 1)}>下一页</button></div></main>;
    }
    else if (route.startsWith('/product/'))
        content = product ? <><div className="local-nav wrap"><a href="#/explore">探索好物</a><span>{product.title}</span><strong>RMB {money(product.price)}</strong></div><main className="detail wrap"><div className="detail-art">{image(product)}<span>商品展示{product.imageUrls.length ? '' : ' · 示意图'}</span>{product.imageUrls.slice(1).map(url => <img className="extra-image" key={url} src={url} alt={`${product.title}补充图片`}/>)}</div><div className="detail-copy"><span className="eyebrow">{product.category} · {product.condition}</span><h1>{product.title}</h1><p className="detail-price">RMB {money(product.price)}</p><span className={`status ${product.status.toLowerCase()}`}>{productLabels[product.status]}</span><p className="description">{product.description}</p><dl><div><dt>发布同学</dt><dd>{product.sellerName}</dd></div><div><dt>发布时间</dt><dd>{new Date(product.createdAt).toLocaleDateString('zh-CN')}</dd></div><div><dt>交接方式</dt><dd>校园当面交接</dd></div></dl>{user?.id === product.sellerId ? <button className="button full" onClick={() => guard('/manage')}>管理我的商品</button> : product.status === 'ON_SALE' ? <TradeRequest product={product}/> : <p className="muted">这件好物暂时无法申请交易。</p>}<p className="fine-print">请在交接前核实商品情况。提交申请后，可在交易中心查看进度。</p></div></main></> : <main className="empty"><h1>这件好物暂未找到。</h1><a href="#/explore" className="text-link">返回探索 ›</a></main>;
    else {
        if (!user)
            return null;
        if (route === '/publish' || route.startsWith('/edit/')) {
            const existing = route.startsWith('/edit/') ? products.find(p => p.id === detailId && p.sellerId === user.id) : undefined;
            content = route.startsWith('/edit/') && (!existing || existing.status === 'SOLD') ? <main className="empty"><h1>此商品无法编辑。</h1><a href="#/manage">返回我的发布</a></main> : <main className="wrap form-page" key={route}>{intro('我的闲置', existing ? '好物信息，随时更新。' : '让闲置，开启下一程。', '认真描述每一处细节，让下一位主人放心心动。')}<ProductEditor key={route} existing={existing} loading={loading} onSubmit={(e, images) => submitProduct(e, images, existing)}/></main>;
        }
        else if (route === '/manage')
            content = <main className="wrap">{intro('我的发布', '每一件，都有下一站。', '管理商品信息，从草稿到售出。')}<div className="section-toolbar"><span>{products.filter(p => p.sellerId === user.id).length} 件我的商品</span><a className="button" href="#/publish">发布闲置</a></div>{products.filter(p => p.sellerId === user.id).map(p => <article className="management-row" key={p.id}><a href={`#/product/${p.id}`} className="mini-art">{image(p)}</a><div><span className="status">{productLabels[p.status]}</span><h3>{p.title}</h3><p>RMB {money(p.price)} <span className="muted">· {p.condition}</span></p></div><div className="row-controls">{p.status !== 'SOLD' && <a href={`#/edit/${p.id}`} className="text-link">编辑</a>}{(p.status === 'DRAFT' || p.status === 'OFF_SHELF') && <button className="text-link" disabled={loading} onClick={() => updateStatus(p, 'ON_SALE')}>上架</button>}{p.status === 'ON_SALE' && <button className="text-link" disabled={loading} onClick={() => updateStatus(p, 'OFF_SHELF')}>下架</button>}</div></article>)}{!products.some(p => p.sellerId === user.id) && <div className="empty"><h2>闲置的下一程，从这里开始。</h2><a href="#/publish" className="text-link">发布第一件好物 ›</a></div>}</main>;
    }
    return content;
}
