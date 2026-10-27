package edu.campus.loop.user;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;

@Component
final class UserResponseFilter implements Filter {
    @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        var http = (HttpServletRequest) request;
        if (http.getRequestURI().startsWith("/api/v1/auth/") || http.getRequestURI().startsWith("/api/v1/users/")) {
            var out = (HttpServletResponse) response;
            out.setHeader("Cache-Control", "no-store");
            out.setHeader("X-Content-Type-Options", "nosniff");
            out.setHeader("X-Trace-Id", UserExceptionHandler.trace(http));
        }
        chain.doFilter(request, response);
    }
}
