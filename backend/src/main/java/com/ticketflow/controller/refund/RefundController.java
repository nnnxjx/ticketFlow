package com.ticketflow.controller.refund;

import com.ticketflow.common.ApiResponse;
import com.ticketflow.interceptor.RequestIdInterceptor;
import com.ticketflow.service.IRefundService;
import com.ticketflow.vo.refund.RefundView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders/{orderId}/refund")
@Tag(name = "整单模拟退款")
@SecurityRequirement(name = "jwtAuth")
public class RefundController {
    @Autowired private IRefundService refunds;

    @PostMapping
    @Operation(summary = "申请本人订单的整单退款")
    public ApiResponse<RefundView> request(@PathVariable long orderId, HttpServletRequest request) {
        return ok(refunds.request(orderId), request);
    }

    @GetMapping
    @Operation(summary = "查询本人订单的最近一次退款")
    public ApiResponse<RefundView> mine(@PathVariable long orderId, HttpServletRequest request) {
        return ok(refunds.mine(orderId), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return ApiResponse.ok(data, (String) request.getAttribute(RequestIdInterceptor.ATTRIBUTE));
    }
}
