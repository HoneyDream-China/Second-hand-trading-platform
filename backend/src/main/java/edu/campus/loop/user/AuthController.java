package edu.campus.loop.user;

import edu.campus.loop.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import static edu.campus.loop.user.UserDtos.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserService service;
    public AuthController(UserService service) { this.service = service; }
    @PostMapping("/register")
    public ApiResponse<UserView> register(@Valid @RequestBody RegisterRequest request) {
        return new ApiResponse<>("OK", "success", service.register(request), null);
    }
    @PostMapping("/login")
    public ApiResponse<LoginView> login(@Valid @RequestBody LoginRequest request) {
        return new ApiResponse<>("OK", "success", service.login(request), null);
    }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() { service.logout(); }
}
