package edu.campus.loop.user;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Repository;
import static edu.campus.loop.user.UserDtos.*;

@Repository
// 主项目启用 JDBC 异常转换后会创建 CGLIB 子类代理，仓储类不能声明为 final。
class UserRepository {
    record StoredUser(UserView view, String passwordHash) { }
    StoredUser byAccount(Connection c, String account) throws SQLException {
        try (var s = c.prepareStatement("SELECT * FROM users WHERE account=?")) {
            s.setString(1, account);
            try (var rows = s.executeQuery()) { return rows.next() ? map(rows) : null; }
        }
    }
    StoredUser byId(Connection c, long id, boolean lock) throws SQLException {
        try (var s = c.prepareStatement("SELECT * FROM users WHERE id=?" + (lock ? " FOR UPDATE" : ""))) {
            s.setLong(1, id);
            try (var rows = s.executeQuery()) { return rows.next() ? map(rows) : null; }
        }
    }
    UserView insert(Connection c, String account, String hash, String nickname, String phone) throws SQLException {
        try (var s = c.prepareStatement("INSERT INTO users(account,password_hash,nickname,phone) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, account); s.setString(2, hash); s.setString(3, nickname); s.setString(4, phone);
            s.executeUpdate();
            try (var keys = s.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Missing generated user id");
                return byId(c, keys.getLong(1), false).view();
            }
        }
    }
    UserView update(Connection c, long id, String nickname, String avatar, String phone) throws SQLException {
        try (var s = c.prepareStatement("UPDATE users SET nickname=?,avatar_url=?,phone=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
            s.setString(1, nickname); s.setString(2, avatar); s.setString(3, phone); s.setLong(4, id);
            s.executeUpdate();
        }
        return byId(c, id, false).view();
    }
    void password(Connection c, long id, String hash) throws SQLException {
        try (var s = c.prepareStatement("UPDATE users SET password_hash=?,updated_at=CURRENT_TIMESTAMP WHERE id=?")) {
            s.setString(1, hash); s.setLong(2, id); s.executeUpdate();
        }
    }
    private StoredUser map(ResultSet row) throws SQLException {
        // 空字符串维持现有公共前端 User 类型兼容；不改其他成员共享类型。
        return new StoredUser(new UserView(row.getLong("id"), row.getString("account"), row.getString("nickname"),
                empty(row.getString("avatar_url")), empty(row.getString("phone")), UserStatus.valueOf(row.getString("status")),
                row.getObject("created_at", OffsetDateTime.class), row.getObject("updated_at", OffsetDateTime.class)), row.getString("password_hash"));
    }
    private String empty(String value) { return value == null ? "" : value; }
}
