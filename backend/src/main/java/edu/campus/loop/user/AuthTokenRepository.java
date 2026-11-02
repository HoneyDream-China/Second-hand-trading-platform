package edu.campus.loop.user;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;

@Repository
// 与 UserRepository 一样，允许 Spring 创建仓储异常转换代理。
class AuthTokenRepository {
    record StoredToken(long userId, OffsetDateTime expiresAt, OffsetDateTime revokedAt) { }
    StoredToken find(Connection c, String hash) throws SQLException {
        try (var s = c.prepareStatement("SELECT user_id,expires_at,revoked_at FROM auth_tokens WHERE token_hash=?")) {
            s.setString(1, hash);
            try (var rows = s.executeQuery()) {
                return rows.next() ? new StoredToken(rows.getLong(1), rows.getObject(2, OffsetDateTime.class), rows.getObject(3, OffsetDateTime.class)) : null;
            }
        }
    }
    void insert(Connection c, long userId, String hash, OffsetDateTime expires) throws SQLException {
        try (var s = c.prepareStatement("INSERT INTO auth_tokens(user_id,token_hash,expires_at) VALUES(?,?,?)")) {
            s.setLong(1, userId); s.setString(2, hash); s.setObject(3, expires); s.executeUpdate();
        }
    }
    void revoke(Connection c, String hash) throws SQLException {
        try (var s = c.prepareStatement("UPDATE auth_tokens SET revoked_at=CURRENT_TIMESTAMP WHERE token_hash=? AND revoked_at IS NULL")) {
            s.setString(1, hash); s.executeUpdate();
        }
    }
    void revokeAll(Connection c, long id) throws SQLException {
        try (var s = c.prepareStatement("UPDATE auth_tokens SET revoked_at=CURRENT_TIMESTAMP WHERE user_id=? AND revoked_at IS NULL")) {
            s.setLong(1, id); s.executeUpdate();
        }
    }
}
