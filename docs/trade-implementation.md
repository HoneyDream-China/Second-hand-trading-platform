# 交易后端实现与接入

`TradeService` 已实现申请、参与者分页查询、详情、接受、拒绝、取消。`TradeRepository` 只访问交易主表和状态日志表；`TradeStateMachine` 集中校验 `PENDING -> ACCEPTED / REJECTED / CANCELLED`。同一终态的重复操作返回原结果，不重复写日志；尝试改为其他终态返回 `409 INVALID_TRADE_TRANSITION`。

## 数据库配置

先在 PostgreSQL 15+ 的空数据库执行 `db/V1__init.sql`（已有表时不要重复执行），再配置 Spring 数据源属性，例如通过环境变量：

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/campus_loop
SPRING_DATASOURCE_USERNAME=campus_loop
SPRING_DATASOURCE_PASSWORD=<本地密码>
```

配置 URL 后，`TradePersistenceConfiguration` 提供共享 DataSource、JdbcTemplate 和事务管理器。未配置时仍能启动原有骨架。SQL 初始化不会自动执行。

## 用户与商品模块接入

当前仓库的用户和商品业务仍为占位实现，交易接口要完成真实联调还必须补齐：

- `CurrentUserContext` 的 Spring Bean：验证登录凭证，并返回当前活跃用户；未登录时抛出对应业务异常。
- `UserFacade.getActiveUser`：查询真实用户并检查活跃状态。
- `ProductFacade.lockProduct`：在调用方事务中执行商品行 `SELECT ... FOR UPDATE`，检查存在性。此方法不要求商品在售，默认实现会明确拒绝未接入的调用。
- `ProductFacade.getTradableProduct`：只返回在售商品快照，其他状态抛出 `409 PRODUCT_NOT_TRADABLE`。
- `ProductFacade.markSold`：按期望版本更新在售商品为 SOLD，递增 version，版本冲突返回 `409 PRODUCT_VERSION_CONFLICT`。

商品锁和商品更新必须使用上述同一 DataSource，并加入调用方事务，不能使用独立事务、独立连接或远程调用。交易创建和接受均先锁商品；接受再锁交易。拒绝与取消只锁交易。商品售出、交易状态和日志任意一步失败都回滚，创建防重依赖原 SQL 中的 `uk_product_active_trade` 部分唯一索引。

缺少认证、数据库或商品实现时仍会返回 `501 NOT_IMPLEMENTED`，不会创建虚构交易。控制器的路径和 DTO 未变；业务异常通过统一响应返回约定状态码、错误码和 traceId。

## 验证

在 backend 运行 `mvn test`。测试使用真实 H2 JDBC 与 Spring 事务及用户/商品替身，覆盖权限、参数校验、参与者过滤、分页、唯一约束、幂等、并发创建/接受、创建日志失败回滚、接受日志失败时商品与交易整体回滚、商品下架、HTTP 错误映射及无数据库启动。

H2 用生成列唯一约束模拟 PostgreSQL 部分唯一索引。测试通过不能替代真实 PostgreSQL 环境和用户、商品实现的联合验证，尤其需要验证商品 Facade 正确加入共享事务并持有行锁。
