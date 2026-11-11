import React from 'react';
import Art from './Art';
import { useApp } from './shared/AppContext';
import { Icon, categories } from './shared/ui';
import UserSubsystem from './modules/user/UserSubsystem';
import ProductSubsystem from './modules/product/ProductSubsystem';
import TradeSubsystem from './modules/trade/TradeSubsystem';
/** 公共首页、导航和路由；三个业务模块由各成员独立维护。 */
export default function App() {
    const { route, user, notice, setNotice, loading, go, guard, setCategory, setPage } = useApp();
    const header = <><header className="global-nav"><nav aria-label="主导航"><a className="brand" href="#/" aria-label="校园循环首页"><Icon name="loop" /><span>校园循环</span></a><a className={route === '/explore' ? 'active' : ''} href="#/explore">探索好物</a><a href="#/explore" onClick={() => { setCategory('数码设备'); setPage(1); }}>数码</a><a href="#/explore" onClick={() => { setCategory('图书教材'); setPage(1); }}>图书</a><a href="#/explore" onClick={() => { setCategory('生活好物'); setPage(1); }}>生活</a><button onClick={() => guard('/publish')}>发布闲置</button><button onClick={() => guard('/trades')}>交易中心</button><button aria-label="搜索商品" onClick={() => { go('/explore'); setTimeout(() => document.getElementById('search')?.focus(), 100); }}><Icon name="search" /></button><button aria-label="个人中心" onClick={() => guard('/account')}>{user?.avatarUrl ? <img className="user-nav-avatar" src={user.avatarUrl} alt="我的头像" /> : <Icon name="user" />}</button></nav></header><div className="announcement">让闲置流动，让校园更有可能。<a href="#/explore">发现你的下一件好物 <span>›</span></a></div></>;
    let content: React.ReactNode;
    if (route === '/')
        content = <>
            <section className="hero"><div className="hero-copy"><span className="eyebrow">好物有下一站。</span><h1>让好物<br />继续发光</h1><p>你的闲置，是另一位同学的心动。<br className="mobile-break" />在校园，遇见刚刚好的拥有。</p><div className="actions"><a className="button" href="#/explore">探索校园好物</a><button className="text-link" onClick={() => guard('/publish')}>发布我的闲置 <span>›</span></button></div></div><div className="hero-art"><span className="hero-word" aria-hidden="true">再一次，心动。</span><Art kind="laptop" /><div className="hero-caption"><span>轻装上阵，开启下一段旅程。</span><a href="#/product/101">发现数码好物 <span>↗</span></a></div></div></section>
            <section className="category-strip wrap"><p>下一件好物，<br /><strong>从这里遇见。</strong></p>{categories.slice(1).map((c, i) => <button key={c} onClick={() => { setCategory(c); setPage(1); go('/explore'); }}><span className="category-symbol">{['⌘', 'Aa', '◒', '↗'][i]}</span>{c}<span className="small-arrow">›</span></button>)}</section>
            <section className="feature dark"><div><span className="eyebrow">数码设备</span><h2>专注自己的世界。<br />听见新的可能。</h2><p>让喜欢的声音，陪你走更远。</p><a className="button" href="#/product/102">了解这件好物</a><span className="feature-price">无线降噪耳机 · RMB 680</span></div><Art kind="headphones" /></section>
            <section className="feature cream"><Art kind="books" /><div><span className="eyebrow">图书与教材</span><h2>知识翻新篇。<br />热爱不毕业。</h2><p>前一页的收获，成为下一页的开始。</p><button className="text-link" onClick={() => { setCategory('图书教材'); go('/explore'); }}>发现好书 <span>›</span></button></div></section>
            <section className="manifesto wrap"><span className="eyebrow">物尽其用，恰逢其人。</span><h2>小小的循环。<br /><span>大大的可能。</span></h2><div className="principles"><div><strong>就在校园。</strong><p>同校发现，当面交接。让好物的下一站，近一点。</p></div><div><strong>每一件，都值得。</strong><p>从一本课本到一台电脑，给闲置一个被需要的机会。</p></div><div><strong>让交易更清楚。</strong><p>从申请到接受，交易状态和时间记录，一目了然。</p></div></div></section>
        </>;
    else if (['/login', '/register'].includes(route))
        content = <UserSubsystem />;
    else if (route === '/explore' || route.startsWith('/product/'))
        content = <ProductSubsystem />;
    else if (['/publish', '/manage', '/trades', '/account'].includes(route) || route.startsWith('/edit/')) {
        if (!user)
            content = <main className="empty"><h1>让好物与你相遇。</h1><p>登录后继续发布、交易和管理个人资料。</p><button className="button" onClick={() => guard(route)}>登录并继续</button></main>;
        else if (route === '/account')
            content = <UserSubsystem />;
        else if (route === '/trades')
            content = <TradeSubsystem />;
        else
            content = <ProductSubsystem />;
    }
    else
        content = <main className="empty"><h1>这一页，暂时还没到站。</h1><a className="text-link" href="#/">回到首页 ›</a></main>;
    return <>{header}{loading && <div role="progressbar" aria-label="正在处理" className="loading-bar" />}{content}<footer><div className="wrap"><p className="footer-note">校园二手商品交易平台 · 请核实商品信息后当面交接。</p><div className="footer-main"><a className="brand" href="#/"><Icon name="loop" />校园循环</a><span>让好物，继续发光。</span><a href="#/explore">探索好物</a><button onClick={() => guard('/publish')}>发布闲置</button><button onClick={() => guard('/account')}>个人中心</button></div><div className="footer-bottom"><span>Copyright © 2026 校园循环。校园课程设计项目。</span><span>用户管理 / 商品管理 / 交易管理</span></div></div></footer>{notice && <div className="toast" role="status">{notice}<button aria-label="关闭提示" onClick={() => setNotice('')}>×</button></div>}</>;
}
