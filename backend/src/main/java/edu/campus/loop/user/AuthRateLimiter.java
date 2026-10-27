package edu.campus.loop.user;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 单实例固定窗口限流。使用直连地址，不信任客户端伪造的转发头。 */
@Component
final class AuthRateLimiter {
    private record Window(long until, int count) { }
    private final Map<String, Window> windows = new HashMap<>();
    synchronized void check(String key, int max, long periodMillis) {
        long now = System.currentTimeMillis();
        if (windows.size() >= 10_000) windows.entrySet().removeIf(entry -> entry.getValue().until <= now);
        var window = windows.get(key);
        if (window == null || window.until <= now) {
            if (windows.size() >= 10_000) throw limited();
            windows.put(key, new Window(now + periodMillis, 1));
        } else {
            if (window.count >= max) throw limited();
            windows.put(key, new Window(window.until, window.count + 1));
        }
    }
    private UserFailure limited() { return new UserFailure(429, "RATE_LIMITED", "操作过于频繁，请稍后重试"); }
}
