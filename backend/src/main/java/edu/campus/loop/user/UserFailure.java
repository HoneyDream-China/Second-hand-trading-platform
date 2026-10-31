package edu.campus.loop.user;

import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

/** 在其他模块调用公开身份接口时，也能保留正确 HTTP 状态。 */
public final class UserFailure extends ResponseStatusException {
    final int status;
    final String code;
    UserFailure(int status, String code, String message) {
        super(HttpStatusCode.valueOf(status), message);
        this.status = status;
        this.code = code;
    }
    public String code() { return code; }
    static UserFailure unauthorized() {
        return new UserFailure(401, "UNAUTHORIZED", "登录已失效，请重新登录");
    }
    static UserFailure invalid(String message) {
        return new UserFailure(400, "VALIDATION_ERROR", message);
    }
}
