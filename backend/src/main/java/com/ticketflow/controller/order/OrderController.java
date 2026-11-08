package com.ticketflow.controller.order;

import com.ticketflow.common.ApiResponse;
import com.ticketflow.dto.order.CreateGeneralOrderRequest;
import com.ticketflow.interceptor.RequestIdInterceptor;
import com.ticketflow.service.IOrderService;
import com.ticketflow.vo.order.OrderPageView;
import com.ticketflow.vo.order.OrderView;
import com.ticketflow.vo.payment.PaymentView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "我的订单")
@SecurityRequirement(name = "jwtAuth")
public class OrderController {
    @Autowired private IOrderService orders;

    @PostMapping("/general")
    @Operation(summary = "无座票同步下单")
    public ApiResponse<OrderView> createGeneral(@Valid @RequestBody CreateGeneralOrderRequest body, HttpServletRequest request) {
        return ok(orders.createGeneral(body), request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查看自己的订单及逐张明细")
    public ApiResponse<OrderView> detail(@PathVariable long id, HttpServletRequest request) {
        return ok(orders.detail(id), request);
    }

    @GetMapping
    @Operation(summary = "分页查询我的订单")
    public ApiResponse<OrderPageView> mine(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size, HttpServletRequest request) {
        return ok(orders.mine(page, size), request);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消本人待支付订单")
    public ApiResponse<OrderView> cancel(@PathVariable long id, HttpServletRequest request) {
        return ok(orders.cancel(id), request);
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "模拟支付本人待支付订单")
    public ApiResponse<PaymentView> pay(@PathVariable long id, HttpServletRequest request) {
        return ok(orders.pay(id), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return ApiResponse.ok(data, (String) request.getAttribute(RequestIdInterceptor.ATTRIBUTE));
    }
}
