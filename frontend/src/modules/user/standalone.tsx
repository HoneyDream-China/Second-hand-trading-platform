import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { UserPages } from './UserSubsystem';
import { userApi, UserApiError } from './api';
import { setToken } from '../../shared/request';
import type { User } from '../../types';
import '../../styles.css';
import './user.css';

function UserStandaloneApp() {
    const [route, setRoute] = useState(location.hash.slice(1) || '/login');
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(false);
    const [restoring, setRestoring] = useState(true);
    const [notice, notify] = useState('');
    const go = (path: string) => { location.hash = path; setRoute(path); };
    const run = async (work: () => Promise<void>) => {
        setLoading(true);
        try { await work(); } catch (error) { notify(error instanceof Error ? error.message : '操作失败'); }
        finally { setLoading(false); }
    };
    useEffect(() => {
        const changed = () => { setRoute(location.hash.slice(1) || '/login'); window.scrollTo(0, 0); };
        window.addEventListener('hashchange', changed);
        return () => window.removeEventListener('hashchange', changed);
    }, []);
    useEffect(() => {
        let active = true;
        const restore = async () => {
            try {
                if (sessionStorage.getItem('campus-token')) {
                    const me = await userApi.me();
                    if (active) { setUser(me); go('/account'); }
                }
            } catch (error) {
                if (active) {
                    if (error instanceof UserApiError && error.status === 401) setToken('');
                    notify(error instanceof Error ? error.message : '登录恢复失败');
                }
            } finally { if (active) setRestoring(false); }
        };
        void restore(); return () => { active = false; };
    }, []);
    useEffect(() => {
        if (!notice) return;
        const timer = setTimeout(() => notify(''), 7000); return () => clearTimeout(timer);
    }, [notice]);
    return <>
        <header className="global-nav"><nav aria-label="用户中心导航">
            <a className="brand" href={user ? '#/account' : '#/login'}>校园循环</a><span className="user-nav-label">成员 A · 用户子系统</span>
            <a href="#/login">登录</a><a href="#/register">注册</a><a href="#/account">个人中心</a>
        </nav></header>
        {(loading || restoring) && <div role="progressbar" aria-label="正在处理" className="loading-bar"/>}
        {restoring ? <main className="empty"><p>正在恢复登录状态…</p></main> : <UserPages runtime={{ route, user, setUser, loading, go, notify, run }} standalone/>}
        <footer><div className="wrap"><p>用户数据由本地后端保存 · 登录有效期 8 小时 · 修改密码后所有会话失效</p><p>独立入口仅加载用户功能，可单独演示与验收。</p></div></footer>
        {notice && <div className="toast" role="status">{notice}<button onClick={() => notify('')} aria-label="关闭提示">×</button></div>}
    </>;
}
createRoot(document.getElementById('root')!).render(<UserStandaloneApp/>);
