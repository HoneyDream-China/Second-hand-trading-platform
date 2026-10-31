# 成员 A · 用户子系统

本目录为成员 A 的运行入口和交付说明。代码直接修改在桌面的 Second-hand-trading-platform 仓库，分支为 `feature/member-a-user-subsystem`。

## 最方便的运行方法

1. 双击本目录的 **Start-Backend.cmd**，等待看到 `Tomcat started on port 8080`。
2. 双击 **Start-Frontend.cmd**，等待看到 Vite 的本地网址。
3. 浏览器打开 **http://127.0.0.1:5173/src/modules/user/index.html**。
4. 点击“创建一个”，注册一个自己的账号，再登录、修改资料、修改密码或退出。

两个窗口都需要保持打开。停止时在各窗口按 Ctrl+C。第一次启动会下载依赖，需要联网。遇到 8080 或 5173 已被占用时，先检查是否已经启动过本系统，不能同时重复启动。

此入口使用真实用户接口，**不需要修改 .env.local**。原首页仍遵循团队原来的演示/服务端模式，不要用原首页是否能运行商品和交易来判断 A 模块是否完成。

### 软件环境

- 用户独立入口支持 JDK 17 / 21；本次使用本机 JDK 17.0.12 验证。原团队 pom.xml 仍要求 Java 21，未修改。
- Node.js 22.12+；本次使用 v24.16.0。
- Maven 3.6.3+；脚本优先发现 PATH 中的 Maven，也支持本机 IDEA 自带 Maven。
- 本机脚本已配置实际发现的 IDEA Maven 路径。换电脑时，可将 Maven 加入 PATH 或设置 MAVEN_HOME，将 JDK 设置为 JAVA_HOME。
- 不需要额外安装数据库即可演示；默认使用 H2 文件数据库。

### IDEA 和 VS Code 中运行

后端：IDEA 打开 `backend/user-pom.xml`，作为独立 Maven 项目导入，选 JDK 17 或 21，运行 `edu.campus.loop.user.UserStandaloneApplication`。工作目录设为 `backend`。

前端：VS Code 打开 `frontend`，执行：

```powershell
npm ci
npm exec vite -- --config src/modules/user/vite.user.config.mjs
```

后端也可以在 backend 目录中用 Maven 命令启动：

```powershell
mvn -f user-pom.xml package -DskipTests
java -jar target/user/campus-user-1.0.0.jar
```

## 数据保存位置

默认数据库位于 `backend/target/user-data/campus-user.mv.db`。账号、密码哈希、登录凭据摘要和资料真实落盘；重启服务后保留。此数据文件不是代码，不提交 Git。

注意：Maven 的 clean 会清理 target，因此可能同时清除默认本地演示数据。需要长期保留时，在启动后端前设置绝对路径，例如：

```powershell
$env:USER_DB_URL='jdbc:h2:file:C:/campus-user-data/campus-user;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=15000'
```

浏览器 Token 存在当前标签页的 sessionStorage，刷新可以恢复登录，关闭标签页后需要重新登录。不要把演示数据中的校园账号理解成已验证学校身份。

## 已完成的接口

前缀 `/api/v1`。除注册和登录外，需要 `Authorization: Bearer <token>`。

| 方法与路径 | 请求内容 | 成功结果 |
|---|---|---|
| POST /auth/register | account、password、nickname、可选 phone | 200 + UserView |
| POST /auth/login | account、password | 200 + token、user、expiresAt |
| POST /auth/logout | 无 | 204 |
| GET /users/me | 无 | 200 + UserView |
| PUT /users/me | nickname、可选 avatarUrl/phone | 200 + UserView |
| PUT /users/me/password | oldPassword、newPassword | 204；全部 Token 撤销 |

账号为 3–64 位字母、数字、下划线、点、短横线，不区分大小写。密码为 8–128 字符，保留空格；昵称去除首尾空白且不能为空。手机号选填，填写时须为 11 位中国大陆手机号（1 开头，第二位为 3–9，剩余为数字）；格式校验不代表已验证号码归属或完成短信验证。选填资料的空字符串和 null 均表示清空；PUT 未提供的选填字段也清空。账号、用户 ID、状态不能通过修改资料变更。

Token 为 32 字节安全随机数的 Base64URL 表示，默认有效期 8 小时。退出只撤销当前 Token；重复退出已知 Token 返回 204。修改密码校验旧密码，并在同一事务内修改密码和撤销该用户全部 Token，随后必须重新登录。

### 异常与限制

| HTTP | code | 含义 |
|---|---|---|
| 400 | VALIDATION_ERROR | 字段或头像 URL 不合法、新旧密码相同 |
| 400 | OLD_PASSWORD_INCORRECT | 当前密码错误 |
| 401 | INVALID_CREDENTIALS | 账号/密码错误或账号不可用 |
| 401 | UNAUTHORIZED | 缺失、伪造、过期、撤销 Token，或用户停用 |
| 404 | ACTIVE_USER_NOT_FOUND | Facade 查询不到活跃用户 |
| 409 | ACCOUNT_EXISTS | 账号重复，包括并发重复 |
| 409 | PASSWORD_CHANGED | 改密过程中密码已被另一个请求修改 |
| 429 | RATE_LIMITED | 触发频率限制，响应含 Retry-After: 60 |
| 500 | INTERNAL_ERROR | 存储等内部错误，响应不暴露 SQL/密码 |

注册：直连 IP 每分钟最多 60 次；登录：直连 IP 每分钟 120 次、单账号每分钟 60 次。使用单进程内存固定窗口，进程重启后重置。集群部署需由团队替换为共享限流设施；本版不宣称具备集群限流。

## 测试和构建

双击 **Run-Tests.cmd**，或在仓库根目录执行：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File member-a/Run-Tests.ps1
```

脚本运行后端真实 HTTP 集成测试、原前端构建和用户独立前端构建。测试使用单独的内存数据库，不删除演示数据库里的用户。

后端报告在 `backend/target/user/surefire-reports/`；并发测试 JSON 位于 `backend/target/user/`；独立前端构建产物位于 `frontend/dist/user/`。详情见[测试报告](测试报告.md)。

## 分工边界与提交

只修改 `backend/src/main/java/edu/campus/loop/user/` 和 `frontend/src/modules/user/` 的原有文件，另外新增用户专用测试、user-pom.xml、本目录脚本和说明。商品、交易代码、共享前端文件、原 pom.xml、application.yml、原数据库脚本和历史 OpenAPI 均保持原样。

不要执行 git add . 把运行数据混入提交。分支合入需要队长审查，不能直接推 main。具体集成事项见[团队集成说明](团队集成说明.md)，开发过程见[开发日志](开发日志.md)。
