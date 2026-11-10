package com.ticketflow.controller.order;

import com.ticketflow.common.ApiResponse;
import com.ticketflow.interceptor.RequestIdInterceptor;
import com.ticketflow.service.IOrderService;
import com.ticketflow.vo.order.OrderPageView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizer/orders")
@Tag(name = "主办方订单查询")
@SecurityRequirement(name = "jwtAuth")
public class OrganizerOrderController {
    @Autowired private IOrderService orders;

    @GetMapping
    @Operation(summary = "分页查询当前负责人名下演出的订单，可按演出筛选")
    public ApiResponse<OrderPageView> list(@RequestParam(required = false) Long eventId,
                                           @RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           HttpServletRequest request) {
        return ApiResponse.ok(orders.organizerOrders(eventId, page, size),
                (String) request.getAttribute(RequestIdInterceptor.ATTRIBUTE));
    }
}
