package com.ticketflow.controller.auth;

import com.ticketflow.dto.auth.LoginRequest;
import com.ticketflow.dto.auth.RegisterRequest;
import com.ticketflow.vo.auth.LoginView;
import com.ticketflow.vo.auth.UserView;
import com.ticketflow.service.IAuthService;
import com.ticketflow.common.ApiResponse;
import com.ticketflow.common.UserContext;
import com.ticketflow.interceptor.RequestIdInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "账号认证")
public class AuthController {
    @Autowired
    private IAuthService service;

    @PostMapping("/register")
    @Operation(summary = "注册用户")
    public ApiResponse<UserView> register(@Valid @RequestBody RegisterRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.register(body), requestId(request));
    }

    @PostMapping("/login")
    @Operation(summary = "登录并获取 JWT")
    public ApiResponse<LoginView> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.login(body), requestId(request));
    }

    @GetMapping("/me")
    @Operation(summary = "查看当前账号")
    @SecurityRequirement(name = "jwtAuth")
    public ApiResponse<UserView> me(HttpServletRequest request) {
        return ApiResponse.ok(service.me(UserContext.getUserId()), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdInterceptor.ATTRIBUTE);
    }
}
