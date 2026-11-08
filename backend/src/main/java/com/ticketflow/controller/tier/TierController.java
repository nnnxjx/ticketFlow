package com.ticketflow.controller.tier;

import com.ticketflow.common.ApiResponse;
import com.ticketflow.dto.tier.TierRequest;
import com.ticketflow.interceptor.RequestIdInterceptor;
import com.ticketflow.service.ITierService;
import com.ticketflow.vo.tier.TierView;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/organizer")
@Tag(name = "票档管理")
@SecurityRequirement(name = "jwtAuth")
public class TierController {
    @Autowired private ITierService tiers;

    @PostMapping("/sessions/{sessionId}/tiers")
    public ApiResponse<TierView> create(@PathVariable long sessionId, @Valid @RequestBody TierRequest body, HttpServletRequest request) {
        return ok(tiers.create(sessionId, body), request);
    }
    @GetMapping("/sessions/{sessionId}/tiers")
    public ApiResponse<List<TierView>> mine(@PathVariable long sessionId, HttpServletRequest request) { return ok(tiers.mine(sessionId), request); }
    @PutMapping("/tiers/{id}")
    public ApiResponse<TierView> update(@PathVariable long id, @Valid @RequestBody TierRequest body, HttpServletRequest request) {
        return ok(tiers.update(id, body), request);
    }
    @DeleteMapping("/tiers/{id}")
    public ApiResponse<Void> delete(@PathVariable long id, HttpServletRequest request) { tiers.delete(id); return ok(null, request); }
    @PostMapping("/tiers/{id}/activate")
    public ApiResponse<TierView> activate(@PathVariable long id, HttpServletRequest request) { return ok(tiers.activate(id), request); }
    @PostMapping("/tiers/{id}/stop")
    public ApiResponse<TierView> stop(@PathVariable long id, HttpServletRequest request) { return ok(tiers.stop(id), request); }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return ApiResponse.ok(data, (String) request.getAttribute(RequestIdInterceptor.ATTRIBUTE));
    }
}
