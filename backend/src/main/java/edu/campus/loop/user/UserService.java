package edu.campus.loop.user;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import static edu.campus.loop.user.UserDtos.*;

@Service
public class UserService implements UserFacade {
    private final UserDatabase database;
    private final UserRepository users;
    private final AuthTokenRepository tokens;
    private final PasswordHasher passwords;
    private final TokenService tokenService;
    private final BearerCurrentUserContext current;
    private final long tokenHours;
    private final String dummyHash;
    public UserService(UserDatabase database, UserRepository users, AuthTokenRepository tokens, PasswordHasher passwords,
                       TokenService tokenService, BearerCurrentUserContext current,
                       @Value("${user.auth.token-hours:8}") long tokenHours) {
        this.database = database; this.users = users; this.tokens = tokens; this.passwords = passwords;
        this.tokenService = tokenService; this.current = current;
        if (tokenHours < 1 || tokenHours > 720) throw new IllegalArgumentException("Token lifetime must be 1..720 hours");
        this.tokenHours = tokenHours;
        this.dummyHash = passwords.hash(tokenService.issue());
    }
    public UserView register(RegisterRequest request) {
        String account = normalizeAccount(request.account());
        if (database.read(c -> users.byAccount(c, account)) != null)
            throw new UserFailure(409, "ACCOUNT_EXISTS", "该账号已注册");
        String hash = passwords.hash(request.password());
        // 数据库 UNIQUE 约束兜底并发重复注册。
        return database.transaction(c -> users.insert(c, account, hash, requiredText(request.nickname()), optional(request.phone())));
    }
    public LoginView login(LoginRequest request) {
        var snapshot = database.read(c -> users.byAccount(c, normalizeAccount(request.account())));
        boolean valid = passwords.matches(request.password(), snapshot == null ? dummyHash : snapshot.passwordHash());
        if (snapshot == null || !valid || snapshot.view().status() != UserStatus.ACTIVE) throw invalidCredentials();
        String raw = tokenService.issue();
        OffsetDateTime expires = OffsetDateTime.now().plusHours(tokenHours);
        return database.transaction(c -> {
            // 与改密共享用户行锁，避免基于旧密码签发新 Token 的竞态。
            var latest = users.byId(c, snapshot.view().id(), true);
            if (latest == null || latest.view().status() != UserStatus.ACTIVE || !latest.passwordHash().equals(snapshot.passwordHash()))
                throw invalidCredentials();
            tokens.insert(c, latest.view().id(), tokenService.hash(raw), expires);
            return new LoginView(raw, latest.view(), expires);
        });
    }
    public void logout() {
        String hash = current.tokenHash();
        database.transaction(c -> {
            var token = tokens.find(c, hash);
            if (token == null) throw UserFailure.unauthorized();
            users.byId(c, token.userId(), true);
            // 已撤销、已过期的已知凭据也返回 204，重复退出幂等。
            tokens.revoke(c, hash);
            return null;
        });
    }
    public UserView me() {
        String hash = current.tokenHash();
        return database.read(c -> current.require(c, hash, false).view());
    }
    public UserView updateMe(UpdateUserRequest request) {
        String hash = current.tokenHash();
        String avatar = optional(request.avatarUrl());
        if (avatar != null) {
            try {
                URI uri = URI.create(avatar);
                if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                        || uri.getHost() == null || uri.getUserInfo() != null) throw UserFailure.invalid("头像必须是有效的 http 或 https 地址");
            } catch (IllegalArgumentException invalid) { throw UserFailure.invalid("头像地址格式不正确"); }
        }
        String finalAvatar = avatar;
        return database.transaction(c -> {
            var user = current.require(c, hash, true).view();
            return users.update(c, user.id(), requiredText(request.nickname()), finalAvatar, optional(request.phone()));
        });
    }
    public void updatePassword(PasswordRequest request) {
        String hash = current.tokenHash();
        var snapshot = database.read(c -> current.require(c, hash, false));
        if (!passwords.matches(request.oldPassword(), snapshot.passwordHash()))
            throw new UserFailure(400, "OLD_PASSWORD_INCORRECT", "当前密码不正确");
        if (request.oldPassword().equals(request.newPassword())) throw UserFailure.invalid("新密码不能与当前密码相同");
        String newHash = passwords.hash(request.newPassword());
        database.transaction(c -> {
            var latest = current.require(c, hash, true);
            if (!latest.passwordHash().equals(snapshot.passwordHash()))
                throw new UserFailure(409, "PASSWORD_CHANGED", "密码已被修改，请重新登录");
            users.password(c, latest.view().id(), newHash);
            tokens.revokeAll(c, latest.view().id());
            return null;
        });
    }
    @Override public ActiveUser getActiveUser(long userId) {
        var user = database.read(c -> users.byId(c, userId, false));
        if (user == null || user.view().status() != UserStatus.ACTIVE)
            throw new UserFailure(404, "ACTIVE_USER_NOT_FOUND", "活跃用户不存在");
        return user.view().active();
    }
    @Override public boolean existsActiveUser(long userId) {
        var user = database.read(c -> users.byId(c, userId, false));
        return user != null && user.view().status() == UserStatus.ACTIVE;
    }
    private UserFailure invalidCredentials() { return new UserFailure(401, "INVALID_CREDENTIALS", "账号或密码不正确，或账号不可用"); }
    private String normalizeAccount(String account) { return account.trim().toLowerCase(Locale.ROOT); }
    private String requiredText(String text) {
        if (text == null || text.isBlank()) throw UserFailure.invalid("昵称不能为空");
        return text.trim();
    }
    private String optional(String text) { return text == null || text.isBlank() ? null : text.trim(); }
}
