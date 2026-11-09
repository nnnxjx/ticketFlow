package com.ticketflow.controller.seat;

import com.ticketflow.common.ApiResponse;
import com.ticketflow.dto.seat.SeatPriceRequest;
import com.ticketflow.interceptor.RequestIdInterceptor;
import com.ticketflow.service.ISeatCatalogService;
import com.ticketflow.vo.seat.SeatView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/organizer")
@Tag(name = "场次座位配置")
@SecurityRequirement(name = "jwtAuth")
public class SeatCatalogController {
    @Autowired private ISeatCatalogService seats;

    @PostMapping("/sessions/{sessionId}/seats/init")
    @Operation(summary = "按场馆模板生成选座场次的独立座位并设置默认价格")
    public ApiResponse<List<SeatView>> initialize(@PathVariable long sessionId, @Valid @RequestBody SeatPriceRequest body, HttpServletRequest request) {
        return ok(seats.initialize(sessionId, body.getPrice()), request);
    }
    @GetMapping("/sessions/{sessionId}/seats")
    @Operation(summary = "查看本主办方场次的座位状态和版本")
    public ApiResponse<List<SeatView>> mine(@PathVariable long sessionId, HttpServletRequest request) { return ok(seats.mine(sessionId), request); }
    @PatchMapping("/seats/{seatId}/price")
    @Operation(summary = "修改草稿场次中空闲座位的价格")
    public ApiResponse<SeatView> changePrice(@PathVariable long seatId, @Valid @RequestBody SeatPriceRequest body, HttpServletRequest request) {
        return ok(seats.changePrice(seatId, body.getPrice()), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return ApiResponse.ok(data, (String) request.getAttribute(RequestIdInterceptor.ATTRIBUTE));
    }
}
