import { request } from '../../shared/request';
import type { Trade, Page } from '../../types';
export const tradeApi = {
    trades: () => request<Page<Trade>>('/trades'),
    trade: (id: number) => request<Trade>(`/trades/${id}`),
    createTrade: (body: unknown) => request<Trade>('/trades', 'POST', body),
    transition: (id: number, action: string) => request<Trade>(`/trades/${id}/${action}`, 'PATCH'),
};
