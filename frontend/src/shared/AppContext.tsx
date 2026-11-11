import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { api } from '../api';
import type { User, Product, Trade } from '../types';
/** 全部数据来自后端接口；刷新页面后从服务端恢复登录态、商品和交易。 */
function useAppState() {
    const [route, setRoute] = useState(location.hash.slice(1) || '/');
    const [user, setUser] = useState<User | null>(null);
    const [products, setProducts] = useState<Product[]>([]);
    const [trades, setTrades] = useState<Trade[]>([]);
    const [notice, setNotice] = useState('');
    const [category, setCategory] = useState('全部好物');
    const [page, setPage] = useState(1);
    const [loading, setLoading] = useState(false);
    const go = (path: string) => { if (location.hash === `#${path}`)
        setRoute(path);
    else
        location.hash = path; };
    const notify = (text: string) => setNotice(text);
    const run = async (fn: () => Promise<void>) => { setLoading(true); try {
        await fn();
    }
    catch (error) {
        notify(error instanceof Error ? error.message : '操作失败');
    }
    finally {
        setLoading(false);
    } };
    useEffect(() => { const handler = () => { setRoute(location.hash.slice(1) || '/'); window.scrollTo(0, 0); }; window.addEventListener('hashchange', handler); return () => window.removeEventListener('hashchange', handler); }, []);
    useEffect(() => { if (notice) {
        const timer = setTimeout(() => setNotice(''), 5000);
        return () => clearTimeout(timer);
    } }, [notice]);
    useEffect(() => {
        void run(async () => {
            const result = await api.products('?size=100');
            setProducts(result.items);
            if (sessionStorage.getItem('campus-token')) {
                setUser(await api.me());
                setTrades((await api.trades()).items);
            }
        });
    }, []);
    useEffect(() => { if (/^\/product\/\d+$/.test(route))
        void run(async () => { const item = await api.product(Number(route.split('/')[2])); setProducts(list => [...list.filter(p => p.id !== item.id), item]); }); }, [route]);
    const guard = (path: string) => { if (user)
        go(path);
    else {
        sessionStorage.setItem('campus-next', path);
        go('/login');
    } };
    return { route, user, setUser, products, setProducts, trades, setTrades, notice, setNotice, category, setCategory, page, setPage, loading, go, notify, run, guard };
}
const AppContext = createContext<ReturnType<typeof useAppState> | null>(null);
export function AppProvider({ children }: {
    children: ReactNode;
}) {
    const value = useAppState();
    return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}
export function useApp() {
    const value = useContext(AppContext);
    if (!value)
        throw new Error('useApp 必须在 AppProvider 中使用');
    return value;
}
