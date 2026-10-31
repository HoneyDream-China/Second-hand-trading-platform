package edu.campus.loop.user;

import edu.campus.loop.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import static edu.campus.loop.user.UserDtos.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService service;
    private final AuthRateLimiter limiter;
    private final HttpServletRequest http;
    public AuthController(UserService service, AuthRateLimiter limiter, HttpServletRequest http) {
        this.service = service; this.limiter = limiter; this.http = http;
    }
    @PostMapping("/register")
    public ApiResponse<UserView> register(@Valid @RequestBody RegisterRequest request) {
        limiter.check("register:" + http.getRemoteAddr(), 60, 60_000);
        return new ApiResponse<>("OK", "success", service.register(request), UserExceptionHandler.trace(http));
    }
    @PostMapping("/login")
    public ApiResponse<LoginView> login(@Valid @RequestBody LoginRequest request) {
        limiter.check("login-ip:" + http.getRemoteAddr(), 120, 60_000);
        limiter.check("login-account:" + request.account().trim().toLowerCase(java.util.Locale.ROOT), 60, 60_000);
        return new ApiResponse<>("OK", "success", service.login(request), UserExceptionHandler.trace(http));
    }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() { service.logout(); }
}
