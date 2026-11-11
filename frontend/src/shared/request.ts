// 前端统一以真实后端接口作为唯一数据来源，不再提供浏览器本地演示模式。
let token = sessionStorage.getItem('campus-token') || '';
export const setToken = (value: string) => { token = value; value ? sessionStorage.setItem('campus-token', value) : sessionStorage.removeItem('campus-token'); };
export async function request<T>(path: string, method = 'GET', body?: unknown): Promise<T> {
    const response = await fetch(`/api/v1${path}`, { method, headers: { ...(body instanceof FormData ? {} : { 'Content-Type': 'application/json' }), ...(token ? { Authorization: `Bearer ${token}` } : {}) }, ...(body === undefined ? {} : { body: body instanceof FormData ? body : JSON.stringify(body) }) });
    if (response.status === 204)
        return undefined as T;
    const data = await response.json();
    if (!response.ok)
        throw new Error(data.message || '服务暂不可用，请稍后重试');
    return data.data;
}
