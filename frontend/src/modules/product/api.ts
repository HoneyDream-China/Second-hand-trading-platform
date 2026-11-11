import { request } from '../../shared/request';
import type { Product, Page } from '../../types';
export const productApi = {
    uploadImage: (file: File) => { const body = new FormData(); body.append('file', file); return request<string>('/products/images', 'POST', body); },
    products: (query = '') => request<Page<Product>>(`/products${query}`),
    product: (id: number) => request<Product>(`/products/${id}`),
    createProduct: (body: unknown) => request<Product>('/products', 'POST', body),
    editProduct: (id: number, body: unknown) => request<Product>(`/products/${id}`, 'PUT', body),
    status: (id: number, body: unknown) => request<Product>(`/products/${id}/status`, 'PATCH', body),
};
