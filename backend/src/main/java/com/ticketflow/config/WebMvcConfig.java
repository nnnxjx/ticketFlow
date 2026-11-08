package com.ticketflow.config;

import com.ticketflow.interceptor.AuthInterceptor;
import com.ticketflow.interceptor.RequestIdInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Autowired private RequestIdInterceptor requestIdInterceptor;
    @Autowired private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestIdInterceptor).addPathPatterns("/api/v1/**");
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/system/ping",
                        "/api/v1/events", "/api/v1/events/*", "/api/v1/events/*/sessions", "/api/v1/sessions/*/tiers", "/api/v1/sessions/*/seats");
    }
}
