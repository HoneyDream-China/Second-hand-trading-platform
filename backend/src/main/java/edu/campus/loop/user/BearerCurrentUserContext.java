package edu.campus.loop.user;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;
import static edu.campus.loop.user.UserDtos.*;

@Component
final class BearerCurrentUserContext implements CurrentUserContext {
    private final HttpServletRequest request;
    private final UserDatabase database;
    private final AuthTokenRepository tokens;
    private final UserRepository users;
    private final TokenService tokenService;
    BearerCurrentUserContext(HttpServletRequest request, UserDatabase database, AuthTokenRepository tokens,
                             UserRepository users, TokenService tokenService) {
        this.request = request; this.database = database; this.tokens = tokens;
        this.users = users; this.tokenService = tokenService;
    }
    String tokenHash() {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.matches("(?i)Bearer [A-Za-z0-9_-]{43}")) throw UserFailure.unauthorized();
        return tokenService.hash(authorization.substring(7));
    }
    @Override public ActiveUser requireUser() {
        return database.read(c -> require(c, tokenHash(), false).view()).active();
    }
    UserRepository.StoredUser require(Connection c, String hash, boolean lock) throws SQLException {
        var token = tokens.find(c, hash);
        if (token == null || token.revokedAt() != null || !token.expiresAt().isAfter(OffsetDateTime.now())) throw UserFailure.unauthorized();
        var user = users.byId(c, token.userId(), lock);
        // 锁等待之后再次检查，避免与退出/改密竞态。
        token = tokens.find(c, hash);
        if (user == null || user.view().status() != UserStatus.ACTIVE || token == null || token.revokedAt() != null
                || !token.expiresAt().isAfter(OffsetDateTime.now())) throw UserFailure.unauthorized();
        return user;
    }
}
