package com.ticketflow.controller.ticket;

import com.ticketflow.common.ApiResponse;
import com.ticketflow.dto.ticket.CheckinRequest;
import com.ticketflow.interceptor.RequestIdInterceptor;
import com.ticketflow.service.ITicketService;
import com.ticketflow.vo.ticket.CheckinView;
import com.ticketflow.vo.ticket.TicketView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Tag(name = "电子票与检票")
@SecurityRequirement(name = "jwtAuth")
public class TicketController {
    @Autowired private ITicketService tickets;

    @GetMapping("/api/v1/orders/{orderId}/tickets")
    @Operation(summary = "查看本人订单的电子票令牌")
    public ApiResponse<List<TicketView>> mine(@PathVariable long orderId, HttpServletRequest request) {
        return ok(tickets.mine(orderId), request);
    }

    @PostMapping("/api/v1/organizer/checkins")
    @Operation(summary = "主办方负责人检票")
    public ApiResponse<CheckinView> checkin(@Valid @RequestBody CheckinRequest body, HttpServletRequest request) {
        return ok(tickets.checkin(body.getToken()), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return ApiResponse.ok(data, (String) request.getAttribute(RequestIdInterceptor.ATTRIBUTE));
    }
}
