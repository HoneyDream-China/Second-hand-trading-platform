package edu.campus.loop.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;

public final class UserDtos {
    private UserDtos() { }
    public enum UserStatus { ACTIVE, DISABLED }
    public record RegisterRequest(@NotBlank @Size(min=3,max=64) @Pattern(regexp="[A-Za-z0-9_.-]+") String account,
            @NotBlank @Size(min=8,max=128) String password,
            @NotBlank(message="昵称不能为空") @Size(max=64, message="昵称不能超过64个字符") String nickname,
            @Pattern(regexp="(?:1[3-9][0-9]{9})?", message="请输入11位中国大陆手机号，或留空") String phone) { }
    public record LoginRequest(@NotBlank @Size(max=64) String account, @NotBlank @Size(max=128) String password) { }
    public record UpdateUserRequest(@NotBlank(message="昵称不能为空") @Size(max=64, message="昵称不能超过64个字符") String nickname,
            @Size(max=512) String avatarUrl,
            @Pattern(regexp="(?:1[3-9][0-9]{9})?", message="请输入11位中国大陆手机号，或留空") String phone) { }
    public record PasswordRequest(@NotBlank @Size(max=128) String oldPassword,
            @NotBlank @Size(min=8,max=128) String newPassword) { }
    public record UserView(long id, String account, String nickname, String avatarUrl,
            String phone, UserStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        public ActiveUser active() { return new ActiveUser(id, nickname, status); }
    }
    public record LoginView(String token, UserView user, OffsetDateTime expiresAt) { }
    public record ActiveUser(long id, String nickname, UserStatus status) { }
}
