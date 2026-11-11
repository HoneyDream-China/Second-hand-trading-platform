import { tradeApi } from './api';
import type { Trade } from '../../types';

/** 进入交易中心后同步当前账号的申请；返回离开页面时的清理函数。 */
export function subscribeTrades(onLoad: (trades: Trade[]) => void, onError: (message: string) => void) {
    let active = true;
    let refreshing = false;
    let failed = false;
    const refresh = async () => {
        if (!active || refreshing || document.visibilityState === 'hidden') return;
        refreshing = true;
        try {
            const items = await tradeApi.allTrades();
            // 离开页面、退出或切换账号后，丢弃旧请求结果。
            if (active) { onLoad(items); failed = false; }
        } catch (error) {
            if (active && !failed) {
                onError(error instanceof Error ? error.message : '交易列表加载失败');
                failed = true;
            }
        } finally {
            refreshing = false;
        }
    };
    const onVisible = () => { void refresh(); };
    void refresh();
    // 买卖双方使用不同会话，卖家停留在页面时也要同步新申请。
    const timer = window.setInterval(onVisible, 5000);
    window.addEventListener('focus', onVisible);
    document.addEventListener('visibilitychange', onVisible);
    return () => {
        active = false;
        window.clearInterval(timer);
        window.removeEventListener('focus', onVisible);
        document.removeEventListener('visibilitychange', onVisible);
    };
}
