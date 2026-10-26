package edu.campus.loop.common;

public class BusinessNotImplementedException extends RuntimeException {
    public BusinessNotImplementedException() {
        super("后端业务逻辑尚未实现，请使用前端演示模式");
    }
}
