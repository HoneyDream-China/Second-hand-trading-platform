# 校园二手商品交易系统接口与数据库设计

## 1. 设计结论

本系统按用户、商品、交易三个业务域拆分。用户域负责身份、登录态和个人资料；商品域负责商品生命周期与图片；交易域负责交易申请、状态流转和并发成交。跨域调用只能通过 Facade 或明确的公开 DTO，禁止直接访问其他模块的 Repository、Entity 或数据库表。

当前版本只覆盖校园二手商品发布、浏览和交易，不包含支付、物流、评论、收藏、即时通信、推荐和后台运营。这些能力以后只能以独立模块接入，不得把字段或业务规则直接塞进现有三个核心域。

## 2. 统一接口约定

### 2.1 基础约定

| 项目 | 约定 |
|---|---|
| Base URL | `/api/v1` |
| 协议 | HTTPS + JSON，UTF-8 |
| 认证 | 除注册、登录、商品公开查询外，默认需要 `Authorization: Bearer <token>` |
| ID | 对外统一使用 `long` 整数；前端不得依赖数据库自增规则 |
| 时间 | 响应使用 ISO 8601，例如 `2026-10-18T10:30:00+08:00`；数据库使用 `timestamptz` |
| 幂等 | 状态操作必须允许重复请求；重复执行返回当前资源状态，不重复写入业务结果 |
| 并发 | 商品更新使用 `version` 乐观锁；版本不匹配返回 `409 PRODUCT_VERSION_CONFLICT` |
| 空值 | 缺失字段不等于空字符串；更新接口采用明确的可选字段语义 |
| 分页 | `page` 从 1 开始，`size` 默认 20，最大 100 |

### 2.2 统一响应体

成功：

```json
{
  "code": "OK",
  "message": "success",
  "data": {},
  "traceId": "b9c0..."
}
```

失败：

```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "商品不存在",
  "data": null,
  "traceId": "b9c0..."
}
```

分页：

```json
{
  "code": "OK",
  "message": "success",
  "data": { "items": [], "page": 1, "size": 20, "total": 0 },
  "traceId": "b9c0..."
}
```

### 2.3 错误码

| HTTP | code | 说明 |
|---:|---|---|
| 400 | `VALIDATION_ERROR` | 参数格式或业务字段校验失败 |
| 401 | `UNAUTHORIZED` | 未登录、Token 无效或已过期 |
| 403 | `FORBIDDEN` | 已登录但无权操作资源 |
| 404 | `RESOURCE_NOT_FOUND` | 资源不存在 |
| 409 | `PRODUCT_VERSION_CONFLICT` | 商品版本冲突 |
| 409 | `PRODUCT_NOT_TRADABLE` | 商品不是可交易状态 |
| 409 | `ACTIVE_TRADE_EXISTS` | 商品已有未结束交易 |
| 409 | `INVALID_TRADE_TRANSITION` | 交易状态不允许该操作 |
| 422 | `BUSINESS_RULE_VIOLATION` | 请求违反业务规则 |
| 500 | `INTERNAL_ERROR` | 未预期的服务端错误 |

## 3. 业务边界与职责

### 3.1 用户域 `user`

负责注册、登录、Token 撤销、当前用户资料和活跃用户校验。对外只输出 `UserView`、`LoginView`，禁止输出 `passwordHash`、Token 明文和认证内部字段。

公开能力：`UserFacade.getActiveUser(userId)`、`UserFacade.existsActiveUser(userId)`、`CurrentUserContext.requireUser()`。

不负责：商品归属、交易权限、商品状态和交易状态。

### 3.2 商品域 `product`

负责商品创建、编辑、查询、上下架、图片顺序和商品版本。只有商品卖家可以编辑自己的商品；已售商品不得修改核心交易字段。商品域维护 `DRAFT -> ON_SALE -> SOLD/OFF_SHELF` 的生命周期。

公开能力：`ProductFacade.getTradableProduct(productId)`、`ProductFacade.markSold(productId, expectedVersion)`。

不负责：创建交易、决定买家权限、写入交易表。

### 3.3 交易域 `trade`

负责交易申请、卖家接受或拒绝、买家取消、交易查询和交易状态日志。接受交易必须在一个数据库事务中校验商品版本、锁定商品、将商品改为 `SOLD`，再将交易改为 `ACCEPTED`；失败时整体回滚。

状态：`PENDING -> ACCEPTED | REJECTED | CANCELLED`。只有商品卖家可以接受或拒绝；只有交易买家可以取消；买家不能申请自己的商品；同一商品最多存在一笔未结束交易。

不负责：用户密码和 Token、商品的通用编辑、支付或线下交付证明。

### 3.4 代码边界

```text
common/        只放响应体、异常、安全上下文、通用校验和配置
user/          controller -> service -> repository；对外暴露 facade/dto
product/       controller -> service -> repository；对外暴露 facade/dto
trade/         controller -> service/state-machine -> repository
```

约束：Controller 不直接访问 Repository；跨模块不引用对方 Entity；事务边界放在 Service；状态迁移集中在 StateMachine；查询返回 DTO/View，不返回 ORM Entity。

## 4. 接口清单

### 4.1 用户接口

| 方法 | 路径 | 权限 | 返回 |
|---|---|---|---|
| POST | `/auth/register` | 公开 | `UserView` |
| POST | `/auth/login` | 公开 | `LoginView` |
| POST | `/auth/logout` | 登录 | 204 |
| GET | `/users/me` | 登录 | `UserView` |
| PUT | `/users/me` | 登录 | `UserView` |
| PUT | `/users/me/password` | 登录 | 204 |

### 4.2 商品接口

| 方法 | 路径 | 权限 | 返回 |
|---|---|---|---|
| POST | `/products` | 登录 | `ProductView` |
| GET | `/products` | 公开 | `Page<ProductView>` |
| GET | `/products/{id}` | 公开 | `ProductDetailView` |
| PUT | `/products/{id}` | 商品卖家 | `ProductView` |
| PATCH | `/products/{id}/status` | 商品卖家 | `ProductView` |

`PUT /products/{id}` 必须携带 `version`；`PATCH /status` 只允许卖家执行合法状态迁移，不接受任意字符串覆盖状态。

查询参数：`page`、`size`、`keyword`、`category`、`minPrice`、`maxPrice`、`condition`、`status`。公开列表默认只返回 `ON_SALE`。

### 4.3 交易接口

| 方法 | 路径 | 权限 | 返回 |
|---|---|---|---|
| POST | `/trades` | 登录 | `TradeView` |
| GET | `/trades` | 登录 | `Page<TradeView>` |
| GET | `/trades/{id}` | 买家或卖家 | `TradeView` |
| PATCH | `/trades/{id}/accept` | 卖家 | `TradeView` |
| PATCH | `/trades/{id}/reject` | 卖家 | `TradeView` |
| PATCH | `/trades/{id}/cancel` | 买家 | `TradeView` |

`POST /trades` 只接收 `productId` 和可选 `message`；服务端从商品快照读取 `sellerId`，客户端不得提交 sellerId、status 或成交价格作为可信字段。

## 5. 数据库设计

数据库建议使用 PostgreSQL 15+。表名和字段统一使用 snake_case；金额使用 `numeric(12,2)`，禁止使用浮点；所有业务表都有 `created_at`，可变业务表有 `updated_at`。

### 5.1 users

用户主表。`account` 是唯一登录标识；`status=ACTIVE` 才能发布商品或发起交易。

| 字段 | 类型 | 约束 |
|---|---|---|
| id | bigint | PK |
| account | varchar(64) | NOT NULL, UNIQUE |
| password_hash | varchar(255) | NOT NULL |
| nickname | varchar(64) | NOT NULL |
| avatar_url | varchar(512) | NULL |
| phone | varchar(32) | NULL |
| status | varchar(16) | ACTIVE / DISABLED |
| created_at / updated_at | timestamptz | NOT NULL |

### 5.2 auth_tokens

只保存 Token 哈希，不保存 Token 明文。注销通过 `revoked_at` 使 Token 立即失效。

| 字段 | 类型 | 约束 |
|---|---|---|
| id | bigint | PK |
| user_id | bigint | FK users(id) |
| token_hash | varchar(128) | NOT NULL, UNIQUE |
| expires_at | timestamptz | NOT NULL |
| revoked_at | timestamptz | NULL |
| created_at | timestamptz | NOT NULL |

### 5.3 products

商品主表。`version` 用于乐观锁；`seller_id` 不可由客户端修改。价格必须大于等于 0。

| 字段 | 类型 | 约束 |
|---|---|---|
| id | bigint | PK |
| seller_id | bigint | FK users(id) |
| title | varchar(120) | NOT NULL |
| description | text | NOT NULL |
| price | numeric(12,2) | NOT NULL, >= 0 |
| category | varchar(64) | NOT NULL |
| condition | varchar(32) | NOT NULL |
| status | varchar(16) | DRAFT / ON_SALE / SOLD / OFF_SHELF |
| version | bigint | NOT NULL, >= 0 |
| created_at / updated_at | timestamptz | NOT NULL |

### 5.4 product_images

商品图片从商品主表拆分，`sort_order` 从 0 开始。同一商品的顺序唯一。

| 字段 | 类型 | 约束 |
|---|---|---|
| id | bigint | PK |
| product_id | bigint | FK products(id) ON DELETE CASCADE |
| url | varchar(512) | NOT NULL |
| sort_order | integer | NOT NULL, >= 0 |
| created_at | timestamptz | NOT NULL |

### 5.5 trades

交易主表。保留 `seller_id` 是为了稳定记录交易双方和提升查询效率；创建时必须与商品当前 seller_id 一致。

| 字段 | 类型 | 约束 |
|---|---|---|
| id | bigint | PK |
| product_id | bigint | FK products(id) |
| buyer_id | bigint | FK users(id) |
| seller_id | bigint | FK users(id) |
| message | varchar(500) | NULL |
| status | varchar(16) | PENDING / ACCEPTED / REJECTED / CANCELLED |
| created_at / updated_at | timestamptz | NOT NULL |

必须约束 `buyer_id <> seller_id`。通过部分唯一索引确保同一商品最多一笔 `PENDING` 交易。

### 5.6 trade_status_logs

不可变的交易状态审计表。每次状态迁移写入一条记录，`operator_id` 是执行迁移的用户。

| 字段 | 类型 | 约束 |
|---|---|---|
| id | bigint | PK |
| trade_id | bigint | FK trades(id) ON DELETE CASCADE |
| from_status | varchar(16) | NULL，仅创建时为空 |
| to_status | varchar(16) | NOT NULL |
| operator_id | bigint | FK users(id) |
| created_at | timestamptz | NOT NULL |

## 6. 事务与并发规则

1. 创建交易：校验当前用户、商品状态和商品归属，插入交易；依赖唯一索引处理并发重复申请。
2. 接受交易：`SELECT ... FOR UPDATE` 锁定商品和交易；校验 `PENDING` 与 `ON_SALE`；更新商品为 `SOLD` 并递增 version；更新交易为 `ACCEPTED`；写入状态日志。
3. 拒绝或取消：锁定交易，按角色和状态机校验后更新状态并写日志，不修改商品状态。
4. 任何一项失败都回滚同一事务，不能出现商品已售出但交易仍为待处理的中间状态。

## 7. 交付和测试边界

每个接口至少覆盖：成功场景、未登录或越权场景、资源不存在场景、重复提交或并发场景。合并前按“注册/登录 -> 发布商品 -> 查询商品 -> 发起交易 -> 接受交易”的顺序跑通集成测试。

后续新增模块必须同时提交：公开 DTO、Facade 方法、状态枚举、数据库迁移、OpenAPI 变更和边界测试；不得直接修改已有模块的内部表或绕过状态机。
