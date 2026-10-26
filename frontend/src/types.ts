export type ProductStatus = 'DRAFT' | 'ON_SALE' | 'OFF_SHELF' | 'SOLD';
export type TradeStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED';
export interface User { id: number; account: string; nickname: string; phone: string; avatarUrl: string; status: 'ACTIVE' | 'DISABLED' }
export interface Product { id: number; sellerId: number; sellerName: string; title: string; description: string; price: string; category: string; condition: string; imageUrls: string[]; art: string; status: ProductStatus; version: number; createdAt: string }
export interface Trade { id: number; productId: number; buyerId: number; sellerId: number; message: string; status: TradeStatus; createdAt: string; logs: { status: TradeStatus; at: string }[] }
export interface Page<T> { items: T[]; page: number; size: number; total: number }
export const productLabels: Record<ProductStatus, string> = { DRAFT: '草稿', ON_SALE: '在售', OFF_SHELF: '已下架', SOLD: '已售出' };
export const tradeLabels: Record<TradeStatus, string> = { PENDING: '待处理', ACCEPTED: '已接受', REJECTED: '已拒绝', CANCELLED: '已取消' };
