/** 成员 A：同一组用户页面同时用于团队应用和独立验收入口。 */
import React from 'react';
import { userApi, UserApiError } from './api';
import { DEMO, setToken } from '../../shared/request';
import { useApp } from '../../shared/AppContext';
import { formValue } from '../../shared/ui';
import { UserHeading } from './UserHeading';
import type { User } from '../../types';
import './user.css';

export interface UserRuntime {
    route: string;
    user: User | null;
    setUser: (user: User | null) => void;
    loading: boolean;
    go: (route: string) => void;
    notify: (message: string) => void;
    run: (work: () => Promise<void>) => Promise<void>;
    clearPrivateData?: () => void;
}
export default function UserSubsystem() {
    const app = useApp();
    return <UserPages runtime={{ ...app, clearPrivateData: () => app.setTrades([]) }} demo={DEMO}/>;
}

// 密码必须保留原始字符，不能使用公共 formValue（它会 trim）。
const passwordValue = (data: FormData, name: string) => String(data.get(name) ?? '');
const phonePattern = '(?:1[3-9][0-9]{9})?';
const phoneHint = '请输入11位中国大陆手机号，或留空';
const validateProfile = (nickname: string, phone: string) => {
    if (!nickname) throw new Error('昵称不能为空');
    if (!new RegExp(`^${phonePattern}$`).test(phone)) throw new Error(phoneHint);
};

export function UserPages({ runtime, demo = false, standalone = false }: {
    runtime: UserRuntime; demo?: boolean; standalone?: boolean;
}) {
    const { route, user, setUser, loading, go, notify, run, clearPrivateData } = runtime;
    const clearSession = () => { setToken(''); setUser(null); clearPrivateData?.(); };
    const execute = (work: () => Promise<void>) => void run(async () => {
        try { await work(); }
        catch (error) {
            if (error instanceof UserApiError && error.code === 'UNAUTHORIZED') {
                clearSession(); go('/login');
            }
            throw error;
        }
    });
    const logout = () => execute(async () => {
        if (!demo) await userApi.logout();
        clearSession(); go('/login'); notify('已退出登录');
    });
    const authenticate = (event: React.FormEvent<HTMLFormElement>, register: boolean) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        execute(async () => {
            const account = formValue(data, 'account');
            const password = passwordValue(data, 'password');
            const nickname = formValue(data, 'nickname');
            const phone = formValue(data, 'phone');
            if (register) validateProfile(nickname, phone);
            if (register && password !== passwordValue(data, 'confirmPassword')) throw new Error('两次输入的密码不一致');
            if (register && !demo) {
                await userApi.register({ account, password, nickname, phone });
                go('/login'); notify('注册成功，请使用新账号登录'); return;
            }
            if (demo) {
                setUser({ id: 1, account, nickname: nickname || '校园同学', phone: '', avatarUrl: '', status: 'ACTIVE' });
            } else {
                const result = await userApi.login({ account, password });
                clearPrivateData?.(); setToken(result.token); setUser(result.user);
            }
            const next = standalone ? '/account' : sessionStorage.getItem('campus-next') || '/account';
            sessionStorage.removeItem('campus-next'); go(next);
            notify(demo ? '已进入前端演示会话' : '登录成功');
        });
    };
    if (route === '/login' || route === '/register') {
        const register = route === '/register';
        return <main className="auth wrap user-surface">
            <UserHeading section={register ? '创建账号' : '用户中心'} title={<>{register ? '开始你的校园' : '欢迎回到校园'}<span className="user-title-accent">循环<span className="user-title-orbit" aria-hidden="true"/></span></>} description={register ? '创建账号，让好物的下一站从这里开始。' : '登录你的账号，让每一次闲置都有新的可能。'}/>
            <form key={route} className="narrow-form" onSubmit={e => authenticate(e, register)}>
                <label>校园账号<input autoComplete="username" name="account" required minLength={3} maxLength={64} pattern="[A-Za-z0-9_.\-]+" placeholder="3–64 位字母、数字、下划线、点或短横线"/></label>
                {register && <label>昵称<input name="nickname" required maxLength={64} autoComplete="nickname"/></label>}
                <label>密码<input type="password" name="password" minLength={8} maxLength={128} autoComplete={register ? 'new-password' : 'current-password'} required placeholder="8–128 位字符，区分大小写"/></label>
                {register && <>
                    <label>确认密码<input type="password" name="confirmPassword" required minLength={8} maxLength={128} autoComplete="new-password"/></label>
                    <label>手机号（选填）<input name="phone" type="tel" maxLength={11} pattern={phonePattern} title={phoneHint} placeholder="11位中国大陆手机号" autoComplete="tel"/></label>
                </>}
                <button className="button full" disabled={loading}>{loading ? '正在处理…' : register ? '创建账号' : '登录'}</button>
            </form>
            <p className="auth-links">{register ? '已有账号？' : '还没有校园账号？'} <a className="text-link" href={register ? '#/login' : '#/register'}>{register ? '立即登录' : '创建一个'} ›</a></p>
            <p className="fine-print">{demo ? '当前为前端演示，密码不会保存或校验。' : '账号不区分大小写。密码中的空格会保留，请妥善保管。'}</p>
        </main>;
    }
    if (!user) return <main className="empty user-surface"><h1>登录你的校园账号。</h1><p>登录后可查看和修改个人资料。</p><a className="button" href="#/login">前往登录</a></main>;
    return <main className="wrap account user-surface">
        <UserHeading section="个人中心" title={<>你好，<span className="user-title-accent">{user.nickname}</span></>} description="管理你的校园身份，让每次登录更安心。"/>
        <div className="account-links">
            {!standalone && <><a href="#/manage">我的发布 <span>↗</span></a><a href="#/trades">我的交易 <span>↗</span></a></>}
            {standalone && <p className="user-session">账号：{user.account} · 状态：正常</p>}
            <button disabled={loading} onClick={logout}>退出登录 <span>↗</span></button>
        </div>
        <div className="account-forms">
            <section><h2>个人资料</h2><p className="muted">校园账号：{user.account}（不可修改）</p>
                <form key={user.id} onSubmit={e => {
                    e.preventDefault(); const data = new FormData(e.currentTarget);
                    execute(async () => {
                        const body = { nickname: formValue(data, 'nickname'), phone: formValue(data, 'phone'), avatarUrl: formValue(data, 'avatarUrl') };
                        validateProfile(body.nickname, body.phone);
                        if (body.avatarUrl && !/^https?:\/\//i.test(body.avatarUrl)) throw new Error('头像地址必须使用 http 或 https');
                        setUser(demo ? { ...user, ...body } : await userApi.updateUser(body)); notify('个人资料已保存');
                    });
                }}>
                    <label>昵称<input name="nickname" required maxLength={64} defaultValue={user.nickname}/></label>
                    <label>手机号（选填）<input name="phone" type="tel" maxLength={11} pattern={phonePattern} title={phoneHint} placeholder="11位中国大陆手机号" autoComplete="tel" defaultValue={user.phone ?? ''}/></label>
                    <label>头像地址（选填）<input name="avatarUrl" type="url" maxLength={512} defaultValue={user.avatarUrl ?? ''} placeholder="https://example.com/avatar.png"/></label>
                    <button className="button" disabled={loading}>保存资料</button>
                </form>
            </section>
            <section><h2>账号安全</h2><p className="muted">修改密码后，所有已登录会话都会失效，需要重新登录。</p>
                <form onSubmit={e => {
                    e.preventDefault(); const form = e.currentTarget; const data = new FormData(form);
                    execute(async () => {
                        if (demo) { notify('演示模式不保存密码，请使用用户子系统独立入口体验真实改密'); return; }
                        const newPassword = passwordValue(data, 'newPassword');
                        if (newPassword !== passwordValue(data, 'confirmPassword')) throw new Error('两次输入的新密码不一致');
                        await userApi.password({ oldPassword: passwordValue(data, 'oldPassword'), newPassword });
                        form.reset(); clearSession(); go('/login'); notify('密码已修改，请重新登录');
                    });
                }}>
                    <label>当前密码<input name="oldPassword" type="password" required maxLength={128} autoComplete="current-password"/></label>
                    <label>新密码<input name="newPassword" type="password" required minLength={8} maxLength={128} autoComplete="new-password" placeholder="8–128 位字符"/></label>
                    <label>确认新密码<input name="confirmPassword" type="password" required minLength={8} maxLength={128} autoComplete="new-password"/></label>
                    <button className="button secondary" disabled={loading}>更新密码</button>
                </form>
            </section>
        </div>
    </main>;
}
