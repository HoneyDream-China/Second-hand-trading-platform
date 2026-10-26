package edu.campus.loop.user;

/** 跨模块只允许调用此公开协议；不得访问用户认证内部字段。 */
public interface UserFacade {
    UserDtos.ActiveUser getActiveUser(long userId);
    boolean existsActiveUser(long userId);
}
