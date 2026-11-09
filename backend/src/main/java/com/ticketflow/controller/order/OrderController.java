package com.ticketflow.controller.order;

import com.ticketflow.common.ApiResponse;
import com.ticketflow.dto.order.CreateGeneralOrderRequest;
import com.ticketflow.dto.order.CreateAssignedOrderRequest;
import com.ticketflow.interceptor.RequestIdInterceptor;
import com.ticketflow.service.IOrderService;
import com.ticketflow.service.IAsyncOrderService;
import com.ticketflow.vo.order.AsyncOrderView;
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
    @Autowired private IAsyncOrderService asyncOrders;

    // 旧同步 HTTP 入口保留供参考；所有票档购票统一由下面的异步入口处理。
    // @PostMapping("/general")
    // public ApiResponse<OrderView> createGeneral(@Valid @RequestBody CreateGeneralOrderRequest body, HttpServletRequest request) {
    //     return ok(orders.createGeneral(body), request);
    // }

    @PostMapping("/general")
    @Operation(summary = "异步受理无座票下单，按 requestId 幂等")
    public ApiResponse<AsyncOrderView> createGeneralAsync(@Valid @RequestBody CreateGeneralOrderRequest body, HttpServletRequest request) {
        return ok(asyncOrders.submit(body), request);
    }

    @PostMapping("/assigned")
    @Operation(summary = "指定场次座位下单，全部锁定成功才创建订单")
    public ApiResponse<OrderView> createAssigned(@Valid @RequestBody CreateAssignedOrderRequest body, HttpServletRequest request) {
        return ok(orders.createAssigned(body), request);
    }

    @GetMapping("/general/async/{requestId}")
    @Operation(summary = "查询本人异步下单结果")
    public ApiResponse<AsyncOrderView> asyncStatus(@PathVariable String requestId, HttpServletRequest request) {
        return ok(asyncOrders.status(requestId), request);
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
