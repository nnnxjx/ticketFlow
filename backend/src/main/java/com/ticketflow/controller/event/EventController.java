package com.ticketflow.controller.event;

import com.ticketflow.common.ApiResponse;
import com.ticketflow.dto.event.EventRequest;
import com.ticketflow.interceptor.RequestIdInterceptor;
import com.ticketflow.service.IEventService;
import com.ticketflow.vo.event.EventView;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/organizer")
@Tag(name = "演出管理")
@SecurityRequirement(name = "jwtAuth")
public class EventController {
    @Autowired private IEventService events;

    @PostMapping("/events")
    public ApiResponse<EventView> create(@Valid @RequestBody EventRequest body, HttpServletRequest request) {
        return ok(events.create(body), request);
    }
    @GetMapping("/events")
    public ApiResponse<List<EventView>> mine(HttpServletRequest request) {
        return ok(events.mine(), request);
    }
    @PutMapping("/events/{id}")
    public ApiResponse<EventView> update(@PathVariable long id, @Valid @RequestBody EventRequest body, HttpServletRequest request) {
        return ok(events.update(id, body), request);
    }
    @DeleteMapping("/events/{id}")
    public ApiResponse<Void> delete(@PathVariable long id, HttpServletRequest request) {
        events.delete(id);
        return ok(null, request);
    }
    @PostMapping("/events/{id}/publish")
    public ApiResponse<EventView> publish(@PathVariable long id, HttpServletRequest request) { return ok(events.publish(id), request); }
    @PostMapping("/events/{id}/off-shelf")
    public ApiResponse<EventView> offShelf(@PathVariable long id, HttpServletRequest request) { return ok(events.offShelf(id), request); }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return ApiResponse.ok(data, (String) request.getAttribute(RequestIdInterceptor.ATTRIBUTE));
    }
}
