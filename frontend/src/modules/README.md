# 前端三人协作分工

前端仍然是一个 React 应用，只启动一次 `npm run dev`。拆分的是源码职责，不是三个独立服务。

## 各成员负责的目录

| 成员 | 目录 | 页面入口 | 接口 | 专用样式 |
|---|---|---|---|---|
| A | `user/` | `UserSubsystem.tsx` | `user/api.ts` | `user/user.css` |
| B | `product/` | `ProductSubsystem.tsx` | `product/api.ts` | `product/product.css` |
| C | `trade/` | `TradeSubsystem.tsx` | `trade/api.ts` | `trade/trade.css` |

- A：登录、注册、退出、个人资料、修改密码表单及交互。
- B：商品探索、筛选、分页、详情、发布、编辑、我的发布、上下架。
- C：交易申请表单 `TradeRequest`、交易中心、买卖角色和状态筛选、接受/拒绝/取消、状态记录。
- 商品详情通过组件引用交易申请表单；修改申请逻辑时，成员 C 修改 `TradeRequest`，无需改商品模块。
- 登录后加载交易列表时，用户模块调用交易模块公开的 `tradeApi`；接口定义由 C 维护。

## 公共文件

以下路径相对 `frontend/src`：

| 文件 | 职责 |
|---|---|
| `main.tsx` | 挂载 React 应用与状态 Provider |
| `App.tsx` | 首页、导航、页脚、登录引导、路由到三个子系统 |
| `shared/AppContext.tsx` | 用户、商品、交易共享状态，会话存储、提示、加载和导航 |
| `shared/request.ts` | 演示/服务端模式、Token、HTTP 请求及错误处理 |
| `shared/ui.tsx` | 图标、页面介绍、商品图片、价格格式化等复用工具 |
| `styles.css` | 全局基础样式、首页和跨模块复用样式 |
| `types.ts` | 三个模块共用的数据类型和状态枚举 |
| `data.ts` | 演示种子数据 |
| `api.ts` | 兼容旧导入的 API 汇总；新增接口优先修改所属模块的 `api.ts` |

公共文件建议指定一人协调维护。变更类型字段、跨模块组件参数或接口签名前与调用方沟通，避免三人同时改同一公共文件。

## Git 提交范围示例

在仓库根目录执行，只暂存本次任务相关文件：

```powershell
# 成员 A 的用户模块任务
git add frontend/src/modules/user/

# 成员 B 的商品模块任务
git add frontend/src/modules/product/

# 成员 C 的交易模块任务
git add frontend/src/modules/trade/
```

如果任务同时修改后端、测试或文档，也要一并检查并提交。不要将本次结构拆分拆成三个不完整的提交交给不同成员：首次拆分应整体合并，三人同步同一个基础版本后再创建各自的功能分支。

## 运行及检查

启动方式保持原样：VS Code 打开 `frontend`，执行 `npm run dev`；构建执行 `npm run build`。后端目录及业务占位函数未改动。

演示数据通过公共状态即时同步：交易接受后商品变为售出；退出后私有页面显示登录引导。用户、商品、交易模块不会各自复制一份数据。
