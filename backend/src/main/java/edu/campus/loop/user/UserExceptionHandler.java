package edu.campus.loop.user;

import edu.campus.loop.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(-10)
@RestControllerAdvice(assignableTypes = {AuthController.class, UserController.class, UserAvatarController.class})
public class UserExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(UserExceptionHandler.class);
    static String trace(HttpServletRequest request) {
        Object trace = request.getAttribute("user.traceId");
        if (trace == null) {
            trace = UUID.randomUUID().toString();
            request.setAttribute("user.traceId", trace);
        }
        return trace.toString();
    }
    @ExceptionHandler(UserFailure.class)
    ResponseEntity<ApiResponse<Void>> business(UserFailure failure, HttpServletRequest request) {
        var builder = ResponseEntity.status(failure.status);
        if (failure.status == 429) builder.header("Retry-After", "60");
        return builder.body(new ApiResponse<>(failure.code, failure.getReason(), null, trace(request)));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            ConstraintViolationException.class})
    ResponseEntity<ApiResponse<Void>> validation(Exception failure, HttpServletRequest request) {
        String message = "请求字段不符合要求";
        if (failure instanceof MethodArgumentNotValidException invalid) {
            message = invalid.getBindingResult().getFieldErrors().stream()
                    .filter(error -> error.getField().equals("nickname") || error.getField().equals("phone"))
                    .map(error -> error.getDefaultMessage()).findFirst().orElse(message);
        }
        return ResponseEntity.badRequest().body(new ApiResponse<>("VALIDATION_ERROR", message, null, trace(request)));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unexpected(Exception failure, HttpServletRequest request) {
        String trace = trace(request);
        // 不记录请求体、密码或 Authorization。
        LOG.error("User request failed traceId={} type={}", trace, failure.getClass().getSimpleName());
        return ResponseEntity.internalServerError().body(new ApiResponse<>("INTERNAL_ERROR", "服务暂不可用，请稍后重试", null, trace));
    }
}
