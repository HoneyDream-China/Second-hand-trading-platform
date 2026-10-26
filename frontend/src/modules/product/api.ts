import { request } from '../../shared/request';
import type { Product, Page } from '../../types';
export const productApi = {
    products: (query = '') => request<Page<Product>>(`/products${query}`),
    product: (id: number) => request<Product>(`/products/${id}`),
    createProduct: (body: unknown) => request<Product>('/products', 'POST', body),
    editProduct: (id: number, body: unknown) => request<Product>(`/products/${id}`, 'PUT', body),
    status: (id: number, body: unknown) => request<Product>(`/products/${id}/status`, 'PATCH', body),
};
