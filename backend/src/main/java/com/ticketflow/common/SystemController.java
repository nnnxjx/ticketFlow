package com.ticketflow.common;

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
        return ApiResponse.ok(Map.of("status", "ready"), (String) request.getAttribute(RequestIdFilter.ATTRIBUTE));
    }
}
