# 本次实现说明

## 页面路由

| Hash 路由 | 页面 |
|---|---|
| `#/` | 首页 |
| `#/explore` | 商品搜索与筛选 |
| `#/product/:id` | 商品详情与申请交易 |
| `#/login` / `#/register` | 登录 / 注册 |
| `#/publish` / `#/edit/:id` | 新建 / 编辑商品 |
| `#/manage` | 我的发布 |
| `#/trades` | 交易中心 |
| `#/account` | 个人资料与账号安全 |

Hash 路由便于 Vite 开发和静态预览，不需要后端处理前端路由。

## 前端模块划分

`main.tsx` 仅挂载应用；`App.tsx` 负责首页、公共导航与路由。业务页面和操作函数分别位于 `modules/user/UserSubsystem.tsx`、`modules/product/ProductSubsystem.tsx`、`modules/trade/TradeSubsystem.tsx`，每个目录还有独立的 `api.ts` 和专用 CSS。交易申请组件 `TradeRequest` 归交易模块，由商品详情引用。共享会话及数据通过 `shared/AppContext.tsx` 提供；网络底层位于 `shared/request.ts`，公共 UI 工具位于 `shared/ui.tsx`。

## 后端接口

所有接口统一使用 `/api/v1`，映射了三份成员设计中的 17 个端点。Service 业务方法统一抛出 501 占位异常。DTO 只提供公开视图，不暴露密码哈希或 Token 哈希。`CurrentUserContext`、`UserFacade`、`ProductFacade` 预留跨模块协议。

原有 `api/openapi.yaml`、`db/V1__init.sql` 和设计文档未修改。数据库迁移不会自动执行，待业务实现时添加 PostgreSQL / Flyway / 持久层依赖。

## 视觉规范

- 字体优先使用系统字体，中文使用 PingFang SC / Microsoft YaHei，无外部字体请求。
- 文字 `#1d1d1f`，次要文字 `#6e6e73`，浅底 `#f5f5f7`，操作蓝 `#0071e3`。
- 整幅展示区和分隔线列表不使用卡片、独立圆角面板或商品阴影。
- 圆角限于按钮、输入框和操作提示。
- 商品 SVG 为本地示意图；发布表单可填写真实图片 URL，图片按照输入顺序展示。

## 真实服务待完成项

除全部业务逻辑外，需要完成请求级认证、授权、错误码及 HTTP 映射、成功响应 traceId、PostgreSQL 映射、私有商品列表契约、完整分页与服务端筛选/排序适配，再进行集成和并发测试。当前服务端模式是可替换的请求边界，并非可上线版本。
