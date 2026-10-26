# 校园循环 · 校园二手商品交易系统

依据成员 A（用户）、成员 B（商品）、成员 C（交易）的设计文档搭建。前端参考 Apple 官网的视觉语言：48px 细导航、超大标题、大面积留白、连续整幅展示、黑白分区、蓝色操作按钮。商品与管理页面使用分隔线列表，不使用卡片网格。品牌及插画为本项目自有设计，不包含 Apple 标志或官网素材。

## 目录与运行方式

```text
software/
├─ frontend/                 VS Code 打开此目录
│  ├─ src/main.tsx           应用启动入口
│  ├─ src/App.tsx            公共首页、导航与路由
│  ├─ src/modules/
│  │  ├─ user/              成员 A：用户页面、接口、专用样式
│  │  ├─ product/           成员 B：商品页面、接口、专用样式
│  │  └─ trade/             成员 C：交易页面、接口、专用样式
│  ├─ src/shared/            公共状态、请求封装和 UI 工具
│  ├─ src/styles.css         公共与首页响应式样式
│  ├─ src/Art.tsx            本地 SVG 商品示意图
│  ├─ src/api.ts             三个模块接口的兼容汇总入口
│  ├─ src/types.ts           类型与状态枚举
│  └─ src/data.ts            演示数据
├─ backend/                  IDEA 打开此 Maven 项目
│  ├─ pom.xml
│  └─ src/main/
│     ├─ java/edu/campus/loop/
│     │  ├─ common/          统一响应、异常、分页对象
│     │  ├─ user/            用户接口、DTO、Service、Facade
│     │  ├─ product/         商品接口、DTO、Service、Facade
│     │  └─ trade/           交易接口、DTO、Service
│     └─ resources/application.yml
├─ api/openapi.yaml          原有接口设计（保留）
├─ db/V1__init.sql           原有 PostgreSQL 数据库设计（保留）
└─ docs/                    原有设计与本次实现说明
```

### 前端：VS Code

安装 Node.js 22.12+（或当前 LTS），用 VS Code 打开 `frontend`，在终端执行：

```powershell
npm install
npm run dev
```

打开终端打印的地址，默认 `http://127.0.0.1:5173`。构建命令为 `npm run build`；产物在 `frontend/dist`。

默认无需后端即可浏览及体验。演示登录可使用任意校园账号及至少 8 位字符的密码，例如账号 `demo`、密码 `campus123`。**演示登录不校验密码，密码不保存；这是前端交互演示，不是真实认证。** 演示数据使用 sessionStorage，关闭标签页会清除；清除浏览器的本站会话数据可恢复初始内容。

### 后端：IntelliJ IDEA

1. 用 IDEA 打开 `backend/pom.xml`，选择作为 Maven 项目导入。
2. Project SDK / Maven Importer / Maven Runner 选择 JDK 21 或更高版本。项目编译目标为 Java 21。
3. Maven 使用 IDEA 自带版本即可，等待依赖同步完成。
4. 运行 `edu.campus.loop.CampusLoopApplication` 的 `main` 方法。
5. 服务监听 `http://localhost:8080`。无需数据库即可启动。

有独立 Maven 时，也可以在 `backend` 执行 `mvn spring-boot:run` 或 `mvn package`。

**按要求，所有后端业务函数均未实现。** Service 方法保留签名与 TODO，并抛出 `BusinessNotImplementedException`；合法格式的请求返回 HTTP 501 / `NOT_IMPLEMENTED`。未连接数据库，未实现认证、密码哈希、Token、持久化或交易事务。控制器映射、DTO、基础格式校验、统一异常响应和应用启动配置仅用于形成可运行的开发骨架。后续实现应替换占位方法。

### 切换真实接口

将 `frontend/.env.example` 复制为 `frontend/.env.local`，设置：

```dotenv
VITE_API_MODE=server
```

重启 Vite。`/api` 由 Vite 代理到 `http://localhost:8080`；后端暂未实现时会显示“后端业务逻辑尚未实现”，不会自动伪造成功或回退演示数据。当前默认模式为 `demo`。生产环境需要额外配置 HTTPS 与 `/api` 反向代理。

## 已实现的前端

前端已按三个子系统拆分，页面、操作函数、接口及专用样式分别放在 `frontend/src/modules/user`、`product`、`trade`。详细分工与公共文件维护规则见 [前端协作说明](frontend/src/modules/README.md)。拆分后启动方式不变。

- 首页：连续全幅商品展示、分类入口、设计理念。
- 探索：关键词、分类、价格区间、成色、价格/时间排序及分页。
- 商品详情：描述、图片、卖家、成色、价格、交易留言与申请。
- 用户：登录、注册、个人资料、修改密码表单、退出登录。
- 我的发布：新建草稿、编辑、上架、下架；售出后不再允许编辑或上架。
- 交易中心：按状态/买卖角色筛选、买家取消、卖家接受/拒绝、状态记录。
- 手机、平板、桌面布局；键盘焦点、输入校验、空状态、错误提示。

演示默认用户 ID 为 1，内置一条该用户作为卖家的待处理交易，可直接体验接受/拒绝。任意演示账号共用同一角色。商品价格使用十进制字符串保存，提交时限制两位小数；后端 DTO 使用 BigDecimal。

## 后续业务实现边界

成员 A 在 `user/UserService` 和 `CurrentUserContext` 实现身份逻辑；成员 B 在 `product/ProductService` 实现商品逻辑；成员 C 在 `trade/TradeService` 实现交易逻辑。跨模块通过 `UserFacade`、`ProductFacade` 调用。需遵循原设计中的乐观锁、部分唯一索引、事务加锁顺序及日志不可变要求。

当前前端模拟不代表并发、数据库事务或访问控制已经实现。真实接口联调时还需根据最终接口补齐“我的商品”所有状态查询和完整分页加载；现有公开商品查询不应被用于绕过私有资源权限。历史 API 文件保持原样，实际联调以三份成员设计及双方确认的 DTO 为准。

当前未加入支付、物流、聊天、评论、推荐或秒杀业务。
