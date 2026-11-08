package com.ticketflow.controller.common;

import com.ticketflow.common.ApiResponse;
import com.ticketflow.interceptor.RequestIdInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {
    @GetMapping("/ping")
    public ApiResponse<Map<String, String>> ping(HttpServletRequest request) {
        return ApiResponse.ok(Map.of("status", "ready"), (String) request.getAttribute(RequestIdInterceptor.ATTRIBUTE));
    }
}
