package edu.campus.loop.user;

/** TODO 校验 Bearer Token 哈希、有效期、撤销状态及活跃用户；失败时拒绝访问。 */
public interface CurrentUserContext {
    UserDtos.ActiveUser requireUser();
}
