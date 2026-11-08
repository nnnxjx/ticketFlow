package com.ticketflow.interceptor;

import com.ticketflow.common.AppException;
import com.ticketflow.common.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

@Component
public class AuthInterceptor implements AsyncHandlerInterceptor {
    @Autowired private JwtDecoder jwtDecoder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContext.clear();
        // NimbusJwtDecoder 用本地密钥验证 HS256 签名；修改 JWT 内容会导致验签失败。
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "请先登录");
        }
        try {
            Jwt jwt = jwtDecoder.decode(authorization.trim());
            UserContext.setUserId(Long.parseLong(jwt.getSubject()));
            return true;
        } catch (JwtException | NumberFormatException ex) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "登录状态无效或已过期");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        UserContext.clear();
    }
}
