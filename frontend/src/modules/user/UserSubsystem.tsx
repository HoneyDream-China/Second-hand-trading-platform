/** 成员 A：登录、注册、个人资料和账号安全。 */
import React from 'react';
import { userApi } from './api';
import { tradeApi } from '../trade/api';
import { DEMO, setToken } from '../../shared/request';
import { useApp } from '../../shared/AppContext';
import { Icon, intro, formValue } from '../../shared/ui';
import './user.css';
export default function UserSubsystem() {
    const { route, user, setUser, setTrades, loading, go, notify, run } = useApp();
    const logout = () => void run(async () => { if (!DEMO)
        await userApi.logout(); setToken(''); setUser(null); go('/'); notify('已退出登录'); });
    const authenticate = (event: React.FormEvent<HTMLFormElement>, register: boolean) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        void run(async () => {
            const account = formValue(data, 'account');
            const password = formValue(data, 'password');
            const nickname = formValue(data, 'nickname');
            if (register) {
                if (!DEMO) {
                    await userApi.register({ account, password, nickname, phone: formValue(data, 'phone') });
                    go('/login');
                    notify('注册成功，请登录');
                    return;
                }
                if (account === 'demo')
                    throw new Error('此演示账号已存在');
            }
            if (DEMO) {
                setUser({ id: 1, account, nickname: nickname || '校园同学', phone: '', avatarUrl: '', status: 'ACTIVE' });
            }
            else {
                const result = await userApi.login({ account, password });
                setToken(result.token);
                setUser(result.user);
                setTrades((await tradeApi.trades()).items);
            }
            const next = sessionStorage.getItem('campus-next') || '/account';
            sessionStorage.removeItem('campus-next');
            go(next);
            notify(DEMO ? '已进入前端演示会话' : '登录成功');
        });
    };
    let content: React.ReactNode;
    if (route === '/login' || route === '/register') {
        const register = route === '/register';
        content = <main className="auth wrap"><Icon name="loop"/>{intro('CAMPUS LOOP', register ? '开始你的校园循环。' : '欢迎回到校园循环。', register ? '创建校园账号，让闲置有新的可能。' : '登录，遇见你的下一件好物。')}<form className="narrow-form" onSubmit={e => authenticate(e, register)}><label>校园账号<input autoComplete="username" name="account" required maxLength={64} placeholder={DEMO ? '演示模式可填写任意账号' : '请输入校园账号'}/></label>{register && <label>昵称<input name="nickname" required maxLength={64} placeholder="大家该怎么称呼你"/></label>}<label>密码<input type="password" name="password" minLength={8} maxLength={128} autoComplete={register ? 'new-password' : 'current-password'} required placeholder="至少 8 位字符"/></label>{register && <label>手机号（选填）<input name="phone" type="tel" maxLength={32}/></label>}<button className="button full" disabled={loading}>{register ? '创建账号' : '登录'}</button></form><p>{register ? '已有账号？' : '还没有校园账号？'} <a className="text-link" href={register ? '#/login' : '#/register'}>{register ? '立即登录' : '创建一个'} ›</a></p>{DEMO && <p className="fine-print">当前为前端演示。账号仅用于本次会话展示，密码不会保存或校验。</p>}</main>;
    }
    else {
        if (!user)
            return null;
        content = <main className="wrap account">{intro('个人中心', `你好，${user.nickname}。`, '你的校园生活，也可以更轻盈。')}<div className="account-links"><a href="#/manage">我的发布 <span>↗</span></a><a href="#/trades">我的交易 <span>↗</span></a><button onClick={logout}>退出登录 <span>↗</span></button></div><div className="account-forms"><section><h2>个人资料</h2><p className="muted">校园账号：{user.account}</p><form onSubmit={e => { e.preventDefault(); const data = new FormData(e.currentTarget); void run(async () => { const body = { nickname: formValue(data, 'nickname'), phone: formValue(data, 'phone'), avatarUrl: formValue(data, 'avatarUrl') }; if (body.avatarUrl && !/^https?:\/\//i.test(body.avatarUrl))
            throw new Error('头像地址必须使用 http 或 https'); setUser(DEMO ? { ...user, ...body } : await userApi.updateUser(body)); notify('个人资料已保存'); }); }}><label>昵称<input name="nickname" required maxLength={64} defaultValue={user.nickname}/></label><label>手机号<input name="phone" type="tel" maxLength={32} defaultValue={user.phone}/></label><label>头像地址<input name="avatarUrl" type="url" maxLength={512} defaultValue={user.avatarUrl}/></label><button className="button" disabled={loading}>保存资料</button></form></section><section><h2>账号安全</h2><p className="muted">定期更新密码，保护校园账号。</p><form onSubmit={e => { e.preventDefault(); const form = e.currentTarget; const data = new FormData(form); void run(async () => { if (DEMO) {
            notify('演示模式不保存密码；修改密码需后端实现后使用');
            return;
        } await userApi.password({ oldPassword: formValue(data, 'oldPassword'), newPassword: formValue(data, 'newPassword') }); form.reset(); notify('密码已修改'); }); }}><label>当前密码<input name="oldPassword" type="password" required autoComplete="current-password"/></label><label>新密码<input name="newPassword" type="password" required minLength={8} maxLength={128} autoComplete="new-password" placeholder="至少 8 位字符"/></label><button className="button secondary" disabled={loading}>更新密码</button></form></section></div></main>;
    }
    return content;
}
