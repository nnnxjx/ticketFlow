package com.ticketflow.controller.session;

import com.ticketflow.common.ApiResponse;
import com.ticketflow.dto.session.SessionRequest;
import com.ticketflow.interceptor.RequestIdInterceptor;
import com.ticketflow.service.ISessionService;
import com.ticketflow.vo.session.SessionView;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/organizer")
@Tag(name = "场次管理")
@SecurityRequirement(name = "jwtAuth")
public class SessionController {
    @Autowired private ISessionService sessions;

    @PostMapping("/events/{eventId}/sessions")
    public ApiResponse<SessionView> create(@PathVariable long eventId, @Valid @RequestBody SessionRequest body, HttpServletRequest request) {
        return ok(sessions.create(eventId, body), request);
    }
    @GetMapping("/events/{eventId}/sessions")
    public ApiResponse<List<SessionView>> mine(@PathVariable long eventId, HttpServletRequest request) { return ok(sessions.mine(eventId), request); }
    @PutMapping("/sessions/{id}")
    public ApiResponse<SessionView> update(@PathVariable long id, @Valid @RequestBody SessionRequest body, HttpServletRequest request) {
        return ok(sessions.update(id, body), request);
    }
    @DeleteMapping("/sessions/{id}")
    public ApiResponse<Void> delete(@PathVariable long id, HttpServletRequest request) { sessions.delete(id); return ok(null, request); }
    @PostMapping("/sessions/{id}/activate")
    public ApiResponse<SessionView> activate(@PathVariable long id, HttpServletRequest request) { return ok(sessions.activate(id), request); }
    @PostMapping("/sessions/{id}/stop")
    public ApiResponse<SessionView> stop(@PathVariable long id, HttpServletRequest request) { return ok(sessions.stop(id), request); }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return ApiResponse.ok(data, (String) request.getAttribute(RequestIdInterceptor.ATTRIBUTE));
    }
}
