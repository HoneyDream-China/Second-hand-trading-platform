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
│  ├─ src/types.ts          类型与状态枚举
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

前端不再内置演示数据：探索、发布、交易、登录等页面全部读取后端接口，请先启动后端再启动前端。首次使用请在注册页创建真实账号，账号由后端写入数据库，登录时按数据库中的密码哈希校验；刷新或重启后数据与登录态都会从服务端恢复。

### 后端：IntelliJ IDEA

1. 用 IDEA 打开 `backend/pom.xml`，选择作为 Maven 项目导入。
2. Project SDK / Maven Importer / Maven Runner 选择 JDK 21 或更高版本。项目编译目标为 Java 21。
3. Maven 使用 IDEA 自带版本即可，等待依赖同步完成。
4. 运行 `edu.campus.loop.CampusLoopApplication` 的 `main` 方法。
5. 服务监听 `http://localhost:8080`。首次启动会自动在 `backend/data` 目录创建 H2 数据库文件并建表，重启后数据保留，无需手动安装数据库。

有独立 Maven 时，也可以在 `backend` 执行 `mvn spring-boot:run` 或 `mvn package`。

用户、商品、交易三个模块均已实现 Service、JDBC 持久化、状态机、权限校验及事务流程。用户模块提供注册、登录、登出、资料与头像修改、改密；商品模块提供草稿、上架、下架、编辑与成交售出；交易模块提供申请、接受、拒绝、取消和状态日志。接口说明见 [交易后端说明](docs/trade-implementation.md)。

### 数据库与数据持久化

后端使用 H2 文件数据库，文件位于 `backend/data/campus-loop.mv.db`，首次启动自动建表，重启不会清空。用户、登录令牌、商品、商品图片、交易和状态日志保存在同一数据库中，并带外键约束。

需要改用其他数据库（如 MySQL / PostgreSQL）时，只需覆盖环境变量，无需改动代码：

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/campus_loop?useSSL=false&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的密码"
```

`DB_URL` 不是 H2 时不会自动建表，需要先自行执行该数据库的建表脚本。`/api` 由 Vite 代理到 `http://localhost:8080`；生产环境需要额外配置 HTTPS 与 `/api` 反向代理。

> H2 是嵌入式数据库，同一时间只能由一个后端进程写入。如果 IDEA 里还留着一个旧实例，新启动的实例会因为拿不到文件锁而以只读方式打开，表现为登录、发布时报“数据库只读”。请先停止旧实例（确认 8080 端口已释放）再启动新实例。

## 已实现的前端

前端已按三个子系统拆分，页面、操作函数、接口及专用样式分别放在 `frontend/src/modules/user`、`product`、`trade`。详细分工与公共文件维护规则见 [前端协作说明](frontend/src/modules/README.md)。拆分后启动方式不变。

- 首页：连续全幅商品展示、分类入口、设计理念。
- 探索：关键词、分类、价格区间、成色、价格/时间排序及分页。
- 商品详情：描述、图片、卖家、成色、价格、交易留言与申请。
- 用户：登录、注册、个人资料、修改密码表单、退出登录。
- 我的发布：新建草稿、编辑、上架、下架；售出后不再允许编辑或上架。
- 交易中心：按状态/买卖角色筛选、买家取消、卖家接受/拒绝、状态记录。
- 手机、平板、桌面布局；键盘焦点、输入校验、空状态、错误提示。

注册后即以该真实账号登录，商品、交易数据和登录态都会从服务端读取；账号、密码哈希与交易记录均落库，不再是浏览器内存数据。

## 后续业务实现边界

成员 A 在 `user/UserService` 和 `CurrentUserContext` 实现身份逻辑；成员 B 在 `product/ProductService` 实现商品逻辑；交易逻辑已在 `trade/TradeService` 实现，后续按 [交易后端说明](docs/trade-implementation.md) 接入认证、数据库和商品 Facade。跨模块通过 `UserFacade`、`ProductFacade` 调用。需遵循原设计中的乐观锁、部分唯一索引、事务加锁顺序及日志不可变要求。

前端已不再模拟数据，所有页面都通过真实接口读写数据库；并发控制（乐观锁、行锁、唯一索引）、事务边界与访问控制均由后端实现。真实接口联调时还需根据最终接口补齐“我的商品”所有状态查询和完整分页加载；现有公开商品查询不应被用于绕过私有资源权限。历史 API 文件保持原样，实际联调以三份成员设计及双方确认的 DTO 为准。

当前未加入支付、物流、聊天、评论、推荐或秒杀业务。
