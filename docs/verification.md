# 验证记录

验证日期：2026-10-21。

## 已通过

- `frontend`：`npm install --registry=https://registry.npmjs.org`，依赖审计 0 个漏洞。
- `frontend`：`npm run build`，TypeScript 检查、Vite 生产构建成功。
- 开发服务 HTTP 首页返回 200。
- 浏览器检查首页视觉、探索页关键词筛选（“相机”返回 1 件商品）、演示登录及个人中心。
- 浏览器操作卖家接受待处理交易后，状态变为“已接受”；对应商品在管理页变为“已售出”，无编辑或上架操作。
- 浏览器操作草稿商品上架后，状态变为“在售”，操作变为“下架”。
- `backend`：通过 IDEA 自带 Maven 执行 `package -DskipTests`，18 个 Java 源文件编译成功，生成 Spring Boot 可执行 JAR。
- `java -jar target/campus-loop-0.1.0.jar` 启动成功，Tomcat 监听 8080。
- `GET /api/v1/products`、`POST /api/v1/auth/login`、`GET /api/v1/trades` 返回 501 / `NOT_IMPLEMENTED`。
- 空注册请求返回 400 / `VALIDATION_ERROR`，带统一响应体及 traceId。

## 范围

上述为构建、启动和代表性交互检查；未进行数据库、真实认证、交易事务或并发测试，因为本次按要求未实现后端业务。响应式 CSS 已编写，未逐一验证全部移动设备尺寸。

首页预览图片：`homepage-preview.png`。

## 前端子系统拆分后的回归检查

- TypeScript 与 Vite 生产构建通过。
- 浏览器演示登录后正常进入个人中心。
- 交易中心接受交易后显示“已接受”，商品详情同步显示“已售出”。
- 商品详情引用独立交易申请组件，提交留言后正常创建演示交易并跳转到交易中心。
- 三个模块保持共享数据；此次未改动后端业务逻辑。
- 回归截图：`subsystems-preview.png`。
