// 兼容原有导入；模块开发请优先编辑各模块的 api.ts。
export { setToken } from './shared/request';
import { userApi } from './modules/user/api';
import { productApi } from './modules/product/api';
import { tradeApi } from './modules/trade/api';
export const api = { ...userApi, ...productApi, ...tradeApi };
