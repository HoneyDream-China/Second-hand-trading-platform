import { request } from '../../shared/request';
import type { User } from '../../types';
export const userApi = {
    login: (body: unknown) => request<{
        token: string;
        user: User;
    }>('/auth/login', 'POST', body),
    register: (body: unknown) => request<User>('/auth/register', 'POST', body),
    logout: () => request<void>('/auth/logout', 'POST'),
    me: () => request<User>('/users/me'),
    updateUser: (body: unknown) => request<User>('/users/me', 'PUT', body),
    password: (body: unknown) => request<void>('/users/me/password', 'PUT', body),
};
