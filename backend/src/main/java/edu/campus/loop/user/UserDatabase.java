package edu.campus.loop.user;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 使用标准 JDBC，不要求修改团队的编译依赖；驱动由 user-pom.xml 提供。 */
@Component
final class UserDatabase {
    private final String url;
    private final String username;
    private final String password;
    private volatile boolean initialized;
    UserDatabase(@Value("${USER_DB_URL:jdbc:h2:file:./target/user-data/campus-user;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;LOCK_TIMEOUT=15000}") String url,
                 @Value("${USER_DB_USERNAME:sa}") String username,
                 @Value("${USER_DB_PASSWORD:}") String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }
    @FunctionalInterface interface Work<T> { T run(Connection connection) throws SQLException; }
    <T> T read(Work<T> work) {
        initialize();
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            return work.run(connection);
        } catch (SQLException failure) { throw translate(failure); }
    }
    <T> T transaction(Work<T> work) {
        initialize();
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            try {
                T result = work.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException failure) {
                connection.rollback();
                throw failure;
            }
        } catch (SQLException failure) { throw translate(failure); }
    }
    private RuntimeException translate(SQLException failure) {
        if ("23505".equals(failure.getSQLState()))
            return new UserFailure(409, "ACCOUNT_EXISTS", "该账号已注册");
        return new IllegalStateException("User persistence failure", failure);
    }
    private synchronized void initialize() {
        if (initialized) return;
        // PostgreSQL 使用团队 V1 脚本建表；这里不修改或初始化其他模块表。
        if (url.startsWith("jdbc:h2:")) {
            try (Connection connection = DriverManager.getConnection(url, username, password);
                 var statement = connection.createStatement()) {
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS users (
                      id BIGSERIAL PRIMARY KEY, account VARCHAR(64) NOT NULL UNIQUE,
                      password_hash VARCHAR(255) NOT NULL, nickname VARCHAR(64) NOT NULL,
                      avatar_url VARCHAR(512), phone VARCHAR(32),
                      status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','DISABLED')),
                      created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP)
                    """);
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS auth_tokens (
                      id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id),
                      token_hash VARCHAR(128) NOT NULL UNIQUE,
                      expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                      revoked_at TIMESTAMP WITH TIME ZONE,
                      created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP)
                    """);
                statement.execute("CREATE INDEX IF NOT EXISTS idx_user_tokens ON auth_tokens(user_id, expires_at)");
            } catch (SQLException failure) { throw translate(failure); }
        }
        initialized = true;
    }
}
