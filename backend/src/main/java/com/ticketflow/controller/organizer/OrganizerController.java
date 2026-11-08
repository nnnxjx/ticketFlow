package com.ticketflow.controller.organizer;

import com.ticketflow.common.ApiResponse;
import com.ticketflow.common.UserContext;
import com.ticketflow.interceptor.RequestIdInterceptor;
import com.ticketflow.dto.organizer.CreateOrganizerRequest;
import com.ticketflow.vo.organizer.OrganizerView;
import com.ticketflow.service.IOrganizerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/v1/organizers")
@Tag(name = "主办方")
@SecurityRequirement(name = "jwtAuth")
public class OrganizerController {
    @Autowired private IOrganizerService service;

    @PostMapping
    @Operation(summary = "创建主办方（当前账号成为负责人）")
    public ApiResponse<OrganizerView> create(@Valid @RequestBody CreateOrganizerRequest body, HttpServletRequest request) {
        return ApiResponse.ok(service.create(UserContext.getUserId(), body.getName()), requestId(request));
    }

    @GetMapping("/me")
    @Operation(summary = "查看当前账号的主办方")
    public ApiResponse<OrganizerView> current(HttpServletRequest request) {
        return ApiResponse.ok(service.current(UserContext.getUserId()), requestId(request));
    }

    private String requestId(HttpServletRequest request) {
        return (String) request.getAttribute(RequestIdInterceptor.ATTRIBUTE);
    }
}
