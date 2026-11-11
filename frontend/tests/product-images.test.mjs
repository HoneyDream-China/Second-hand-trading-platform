import { test } from 'node:test';
import assert from 'node:assert/strict';
import { build } from 'esbuild';
import { fileURLToPath } from 'node:url';
import { renderToStaticMarkup } from 'react-dom/server';

globalThis.sessionStorage = { getItem: () => 'seller-token' };
const compiled = await build({
    stdin: {
        contents: "export { productApi } from './src/modules/product/api'; export { image } from './src/shared/ui';",
        resolveDir: fileURLToPath(new URL('..', import.meta.url)),
    },
    bundle: true, format: 'esm', write: false, platform: 'browser',
});
const { productApi, image } = await import(`data:text/javascript;base64,${Buffer.from(compiled.outputFiles[0].text).toString('base64')}`);

test('本地图片通过带登录凭据的 multipart 上传并取得持久地址', async () => {
    const original = globalThis.fetch;
    try {
        globalThis.fetch = async (url, options) => {
            assert.equal(url, '/api/v1/products/images');
            assert.equal(options.method, 'POST');
            assert.equal(options.headers.Authorization, 'Bearer seller-token');
            assert.equal(options.headers['Content-Type'], undefined);
            assert.ok(options.body instanceof FormData);
            assert.equal(options.body.get('file').name, 'photo.png');
            return { ok: true, status: 200, json: async () => ({ data: '/api/v1/products/images/saved.png' }) };
        };
        assert.equal(await productApi.uploadImage(new File(['photo'], 'photo.png', { type: 'image/png' })), '/api/v1/products/images/saved.png');
        globalThis.fetch = async () => ({ ok: false, status: 400, json: async () => ({ message: '图片损坏或无法读取' }) });
        await assert.rejects(productApi.uploadImage(new File(['bad'], 'bad.png')), /图片损坏或无法读取/);
    } finally { globalThis.fetch = original; }
});

test('探索浏览图使用首张图片，移除首图后使用下一张', () => {
    const product = { title: '校园好物', imageUrls: ['/first.png', '/second.png'] };
    const markup = renderToStaticMarkup(image(product));
    assert.match(markup, /src="\/first.png"/);
    assert.doesNotMatch(markup, /second.png/);
    assert.match(renderToStaticMarkup(image({ ...product, imageUrls: product.imageUrls.slice(1) })), /src="\/second.png"/);
});
