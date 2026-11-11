package edu.campus.loop.common;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public ApiResponse<Void> uploadTooLarge() {
        return new ApiResponse<>("VALIDATION_ERROR", "上传文件过大，商品图片每张不能超过 5MB", null, UUID.randomUUID().toString());
    }
    @ExceptionHandler(BusinessException.class)
    public org.springframework.http.ResponseEntity<ApiResponse<Void>> business(BusinessException exception) {
        return org.springframework.http.ResponseEntity.status(exception.status()).body(
                new ApiResponse<>(exception.code(), exception.getMessage(), null, UUID.randomUUID().toString()));
    }

    @ExceptionHandler(BusinessNotImplementedException.class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public ApiResponse<Void> notImplemented(BusinessNotImplementedException exception) {
        return new ApiResponse<>("NOT_IMPLEMENTED", exception.getMessage(), null, UUID.randomUUID().toString());
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
            MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> validation(Exception exception) {
        return new ApiResponse<>("VALIDATION_ERROR", "请求参数格式不正确", null, UUID.randomUUID().toString());
    }
}
