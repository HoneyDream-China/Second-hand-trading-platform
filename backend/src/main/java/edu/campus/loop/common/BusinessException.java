package edu.campus.loop.common;

import org.springframework.http.HttpStatus;

/** 可向客户端返回的业务错误，保留接口约定的 HTTP 状态与错误码。 */
public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() { return status; }
    public String code() { return code; }
}
