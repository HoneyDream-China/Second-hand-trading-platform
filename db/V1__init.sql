-- PostgreSQL 15+
CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    account      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname     VARCHAR(64)  NOT NULL,
    avatar_url   VARCHAR(512),
    phone        VARCHAR(32),
    status       VARCHAR(16) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE auth_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id),
    token_hash  VARCHAR(128) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked_at  TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_auth_tokens_user_active ON auth_tokens(user_id, expires_at)
    WHERE revoked_at IS NULL;

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    seller_id   BIGINT NOT NULL REFERENCES users(id),
    title       VARCHAR(120) NOT NULL,
    description TEXT NOT NULL,
    price       NUMERIC(12,2) NOT NULL CHECK (price >= 0),
    category    VARCHAR(64) NOT NULL,
    condition   VARCHAR(32) NOT NULL,
    status      VARCHAR(16) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'ON_SALE', 'SOLD', 'OFF_SHELF')),
    version     BIGINT NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_products_seller_status ON products(seller_id, status);
CREATE INDEX idx_products_status_created ON products(status, created_at DESC);

CREATE TABLE product_images (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    url         VARCHAR(512) NOT NULL,
    sort_order  INTEGER NOT NULL CHECK (sort_order >= 0),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(product_id, sort_order)
);
CREATE INDEX idx_product_images_product ON product_images(product_id, sort_order);

CREATE TABLE trades (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT NOT NULL REFERENCES products(id),
    buyer_id    BIGINT NOT NULL REFERENCES users(id),
    seller_id   BIGINT NOT NULL REFERENCES users(id),
    message     VARCHAR(500),
    status      VARCHAR(16) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (buyer_id <> seller_id)
);
CREATE INDEX idx_trades_buyer_status ON trades(buyer_id, status, created_at DESC);
CREATE INDEX idx_trades_seller_status ON trades(seller_id, status, created_at DESC);
CREATE INDEX idx_trades_product ON trades(product_id, created_at DESC);
CREATE UNIQUE INDEX uk_product_active_trade ON trades(product_id)
    WHERE status = 'PENDING';

CREATE TABLE trade_status_logs (
    id            BIGSERIAL PRIMARY KEY,
    trade_id      BIGINT NOT NULL REFERENCES trades(id) ON DELETE CASCADE,
    from_status   VARCHAR(16),
    to_status     VARCHAR(16) NOT NULL
        CHECK (to_status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    operator_id   BIGINT NOT NULL REFERENCES users(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_trade_logs_trade_created ON trade_status_logs(trade_id, created_at);
