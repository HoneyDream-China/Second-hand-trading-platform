package edu.campus.loop.user;

import edu.campus.loop.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import static edu.campus.loop.user.UserDtos.*;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final UserService service;
    private final HttpServletRequest http;
    public UserController(UserService service, HttpServletRequest http) { this.service = service; this.http = http; }
    @GetMapping
    public ApiResponse<UserView> me() { return new ApiResponse<>("OK", "success", service.me(), UserExceptionHandler.trace(http)); }
    @PutMapping
    public ApiResponse<UserView> update(@Valid @RequestBody UpdateUserRequest request) {
        return new ApiResponse<>("OK", "success", service.updateMe(request), UserExceptionHandler.trace(http));
    }
    @PutMapping("/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@Valid @RequestBody PasswordRequest request) { service.updatePassword(request); }
}
