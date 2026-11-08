package com.ticketflow.controller.refund;

import com.ticketflow.common.ApiResponse;
import com.ticketflow.interceptor.RequestIdInterceptor;
import com.ticketflow.service.IRefundService;
import com.ticketflow.vo.refund.RefundView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("dev")
@RequestMapping("/api/v1/orders/{orderId}/refund")
@SecurityRequirement(name = "jwtAuth")
public class RefundSimulationController {
    @Autowired private IRefundService refunds;

    @PostMapping("/simulate-failure")
    @Operation(summary = "开发环境模拟退款失败并恢复订单")
    public ApiResponse<RefundView> fail(@PathVariable long orderId, HttpServletRequest request) {
        // 先按当前用户读取退款申请，防止使用他人的 refundId 调用内部恢复方法。
        RefundView owned = refunds.mine(orderId);
        RefundView result = refunds.fail(owned.getId(), "SIMULATED_FAILURE");
        return ApiResponse.ok(result, (String) request.getAttribute(RequestIdInterceptor.ATTRIBUTE));
    }
}
