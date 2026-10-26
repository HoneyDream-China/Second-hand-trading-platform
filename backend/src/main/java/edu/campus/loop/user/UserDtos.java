package edu.campus.loop.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public final class UserDtos {
    private UserDtos() { }
    public enum UserStatus { ACTIVE, DISABLED }
    public record RegisterRequest(@NotBlank @Size(max=64) String account,
            @NotBlank @Size(min=8,max=128) String password,
            @NotBlank @Size(max=64) String nickname, @Size(max=32) String phone) { }
    public record LoginRequest(@NotBlank String account, @NotBlank String password) { }
    public record UpdateUserRequest(@NotBlank @Size(max=64) String nickname,
            @Size(max=512) String avatarUrl, @Size(max=32) String phone) { }
    public record PasswordRequest(@NotBlank String oldPassword,
            @NotBlank @Size(min=8,max=128) String newPassword) { }
    public record UserView(long id, String account, String nickname, String avatarUrl,
            String phone, UserStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) { }
    public record LoginView(String token, UserView user) { }
    public record ActiveUser(long id, String nickname, UserStatus status) { }
}
