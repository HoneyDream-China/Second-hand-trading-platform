package edu.campus.loop.user;

import edu.campus.loop.common.BusinessNotImplementedException;
import org.springframework.stereotype.Service;
import static edu.campus.loop.user.UserDtos.*;

@Service
public class UserService implements UserFacade {
    // TODO 成员 A：密码哈希、Token 哈希、注销立即失效、频率限制及身份校验。
    public UserView register(RegisterRequest request) { throw new BusinessNotImplementedException(); }
    public LoginView login(LoginRequest request) { throw new BusinessNotImplementedException(); }
    public void logout() { throw new BusinessNotImplementedException(); }
    public UserView me() { throw new BusinessNotImplementedException(); }
    public UserView updateMe(UpdateUserRequest request) { throw new BusinessNotImplementedException(); }
    public void updatePassword(PasswordRequest request) { throw new BusinessNotImplementedException(); }
    public ActiveUser getActiveUser(long userId) { throw new BusinessNotImplementedException(); }
    public boolean existsActiveUser(long userId) { throw new BusinessNotImplementedException(); }
}
