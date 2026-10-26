package edu.campus.loop.user;

import edu.campus.loop.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import static edu.campus.loop.user.UserDtos.*;

@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service = service; }
    @GetMapping
    public ApiResponse<UserView> me() { return new ApiResponse<>("OK", "success", service.me(), null); }
    @PutMapping
    public ApiResponse<UserView> update(@Valid @RequestBody UpdateUserRequest request) {
        return new ApiResponse<>("OK", "success", service.updateMe(request), null);
    }
    @PutMapping("/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@Valid @RequestBody PasswordRequest request) { service.updatePassword(request); }
}
