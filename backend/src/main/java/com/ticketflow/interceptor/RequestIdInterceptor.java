package com.ticketflow.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import java.util.UUID;

@Component
public class RequestIdInterceptor implements AsyncHandlerInterceptor {
    public static final String ATTRIBUTE = "ticketflow.requestId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只接受安全字符组成的外部请求 ID，避免污染响应头和日志。
        String incoming = request.getHeader("X-Request-Id");
        String requestId = incoming != null && incoming.matches("[A-Za-z0-9_-]{1,64}")
                ? incoming : UUID.randomUUID().toString();
        request.setAttribute(ATTRIBUTE, requestId);
        response.setHeader("X-Request-Id", requestId);
        MDC.put("requestId", requestId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove("requestId");
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        MDC.remove("requestId");
    }
}
