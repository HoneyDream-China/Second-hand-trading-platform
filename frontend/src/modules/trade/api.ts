import { request } from '../../shared/request';
import type { Trade, Page } from '../../types';
export const tradeApi = {
    trades: () => request<Page<Trade>>('/trades'),
    // 交易中心在客户端筛选状态和角色，因此必须读取全部分页。
    allTrades: async (): Promise<Trade[]> => {
        const trades: Trade[] = [];
        let page = 1;
        let result: Page<Trade>;
        do {
            result = await request<Page<Trade>>(`/trades?page=${page}&size=100`);
            trades.push(...result.items);
            page++;
        } while (result.items.length > 0 && trades.length < result.total);
        return [...new Map(trades.map(trade => [trade.id, trade])).values()];
    },
    trade: (id: number) => request<Trade>(`/trades/${id}`),
    createTrade: (body: unknown) => request<Trade>('/trades', 'POST', body),
    transition: (id: number, action: string) => request<Trade>(`/trades/${id}/${action}`, 'PATCH'),
};
