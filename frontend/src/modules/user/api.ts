import type { User } from '../../types';

export interface RegisterInput { account: string; password: string; nickname: string; phone?: string }
export interface LoginInput { account: string; password: string }
export interface ProfileInput { nickname: string; avatarUrl: string; phone: string }
export interface PasswordInput { oldPassword: string; newPassword: string }
export class UserApiError extends Error {
    constructor(message: string, public status: number, public code: string, public traceId?: string) { super(message); }
}
async function request<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
    const token = sessionStorage.getItem('campus-token');
    let response: Response;
    try {
        response = await fetch(`/api/v1${path}`, {
            method, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
            ...(body === undefined ? {} : { body: JSON.stringify(body) }),
            signal: AbortSignal.timeout(30_000),
        });
    } catch { throw new UserApiError('无法连接用户服务，请确认后端已启动后重试', 0, 'NETWORK_ERROR'); }
    if (response.status === 204) return undefined as T;
    const data = await response.json().catch(() => null);
    if (!response.ok) throw new UserApiError(data?.message || '用户服务暂不可用', response.status, data?.code || 'REQUEST_FAILED', data?.traceId);
    return data.data as T;
}
export const userApi = {
    login: (body: LoginInput) => request<{
        token: string;
        user: User;
        expiresAt: string;
    }>('/auth/login', 'POST', body),
    register: (body: RegisterInput) => request<User>('/auth/register', 'POST', body),
    logout: () => request<void>('/auth/logout', 'POST'),
    me: () => request<User>('/users/me'),
    updateUser: (body: ProfileInput) => request<User>('/users/me', 'PUT', body),
    password: (body: PasswordInput) => request<void>('/users/me/password', 'PUT', body),
};
