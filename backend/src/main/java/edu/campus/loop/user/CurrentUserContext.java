package edu.campus.loop.user;

/** 校验当前请求的 Bearer Token、有效期、撤销状态及活跃用户；失败时拒绝访问。 */
public interface CurrentUserContext {
    UserDtos.ActiveUser requireUser();
}
