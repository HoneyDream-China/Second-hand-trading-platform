import { beforeEach, afterEach, test } from 'node:test';
import assert from 'node:assert/strict';
import { build } from 'esbuild';
import { fileURLToPath } from 'node:url';

// 使用项目已有的编译器运行 TypeScript，无需新增测试依赖。
globalThis.sessionStorage = { getItem: () => 'seller-token' };
const compiled = await build({
    stdin: {
        contents: "export { tradeApi } from './src/modules/trade/api'; export { subscribeTrades } from './src/modules/trade/sync';",
        resolveDir: fileURLToPath(new URL('..', import.meta.url)),
    },
    bundle: true, format: 'esm', write: false, platform: 'node',
});
const { tradeApi, subscribeTrades } = await import(`data:text/javascript;base64,${Buffer.from(compiled.outputFiles[0].text).toString('base64')}`);
const settle = () => new Promise(resolve => setImmediate(resolve));
const response = items => ({ ok: true, status: 200, json: async () => ({ data: { items, total: items.length } }) });
let timers, cleanup;
const originalFetch = globalThis.fetch;

beforeEach(() => {
    timers = new Map();
    globalThis.window = new EventTarget();
    window.setInterval = (callback, delay) => { assert.equal(delay, 5000); timers.set(1, callback); return 1; };
    window.clearInterval = id => timers.delete(id);
    globalThis.document = new EventTarget();
    document.visibilityState = 'visible';
});
afterEach(() => {
    cleanup?.(); cleanup = undefined;
    globalThis.fetch = originalFetch;
    delete globalThis.window;
    delete globalThis.document;
});

test('进入交易中心读取申请，买家新增申请后卖家无需刷新页面', async () => {
    let rows = [];
    const snapshots = [];
    globalThis.fetch = async (_url, options) => {
        assert.equal(options.headers.Authorization, 'Bearer seller-token');
        return response(rows);
    };
    cleanup = subscribeTrades(items => snapshots.push(items), assert.fail);
    await settle();
    assert.deepEqual(snapshots, [[]]);
    rows = [{ id: 1, buyerId: 10, sellerId: 20, status: 'PENDING' }];
    timers.get(1)();
    await settle();
    assert.deepEqual(snapshots.at(-1), rows);
});

test('返回窗口、恢复可见时更新，后台标签不轮询', async () => {
    let calls = 0;
    globalThis.fetch = async () => { calls++; return response([]); };
    cleanup = subscribeTrades(() => {}, assert.fail);
    await settle();
    document.visibilityState = 'hidden';
    timers.get(1)();
    assert.equal(calls, 1);
    document.visibilityState = 'visible';
    document.dispatchEvent(new Event('visibilitychange'));
    await settle();
    window.dispatchEvent(new Event('focus'));
    await settle();
    assert.equal(calls, 3);
});

test('读取后续分页，避免待处理申请被前100条历史记录遮住', async () => {
    const history = Array.from({ length: 100 }, (_, i) => ({ id: i + 1, status: 'REJECTED' }));
    const pending = { id: 101, status: 'PENDING' };
    const urls = [];
    globalThis.fetch = async url => {
        urls.push(url);
        return { ok: true, json: async () => ({ data: { items: urls.length === 1 ? history : [pending], total: 101 } }) };
    };
    const rows = await tradeApi.allTrades();
    assert.equal(rows.length, 101);
    assert.deepEqual(rows.filter(t => t.status === 'PENDING'), [pending]);
    assert.deepEqual(urls, ['/api/v1/trades?page=1&size=100', '/api/v1/trades?page=2&size=100']);
});

test('离开或切换账号后丢弃旧响应，移除定时器和监听器', async () => {
    let resolveFetch;
    let calls = 0;
    globalThis.fetch = () => { calls++; return new Promise(resolve => { resolveFetch = resolve; }); };
    cleanup = subscribeTrades(assert.fail, assert.fail);
    timers.get(1)();
    window.dispatchEvent(new Event('focus'));
    assert.equal(calls, 1, '刷新请求不应重叠');
    cleanup();
    resolveFetch(response([{ id: 1 }]));
    await settle();
    assert.equal(timers.size, 0);
    window.dispatchEvent(new Event('focus'));
    document.dispatchEvent(new Event('visibilitychange'));
    assert.equal(calls, 1);
});

test('请求失败保留已有数据，下次轮询可恢复', async () => {
    const snapshots = [], errors = [];
    let broken = false;
    globalThis.fetch = async () => {
        if (broken) throw new Error('网络中断');
        return response([{ id: 1 }]);
    };
    cleanup = subscribeTrades(items => snapshots.push(items), message => errors.push(message));
    await settle();
    broken = true;
    timers.get(1)(); await settle();
    timers.get(1)(); await settle();
    assert.equal(snapshots.length, 1);
    assert.deepEqual(errors, ['网络中断']);
    broken = false;
    timers.get(1)(); await settle();
    assert.equal(snapshots.length, 2);
});
