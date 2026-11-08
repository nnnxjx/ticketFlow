package com.ticketflow.controller.event;

import com.ticketflow.common.ApiResponse;
import com.ticketflow.interceptor.RequestIdInterceptor;
import com.ticketflow.service.IEventService;
import com.ticketflow.service.ISessionService;
import com.ticketflow.service.ITierService;
import com.ticketflow.service.ISeatCatalogService;
import com.ticketflow.vo.event.EventView;
import com.ticketflow.vo.session.SessionView;
import com.ticketflow.vo.tier.TierView;
import com.ticketflow.vo.seat.SeatView;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "演出浏览")
public class PublicCatalogController {
    @Autowired private IEventService events;
    @Autowired private ISessionService sessions;
    @Autowired private ITierService tiers;
    @Autowired private ISeatCatalogService seats;

    @GetMapping("/events")
    public ApiResponse<List<EventView>> events(HttpServletRequest request) { return ok(events.publicList(), request); }
    @GetMapping("/events/{id}")
    public ApiResponse<EventView> event(@PathVariable long id, HttpServletRequest request) { return ok(events.publicDetail(id), request); }
    @GetMapping("/events/{eventId}/sessions")
    public ApiResponse<List<SessionView>> sessions(@PathVariable long eventId, HttpServletRequest request) {
        return ok(sessions.publicList(eventId), request);
    }
    @GetMapping("/sessions/{sessionId}/tiers")
    public ApiResponse<List<TierView>> tiers(@PathVariable long sessionId, HttpServletRequest request) { return ok(tiers.publicList(sessionId), request); }
    @GetMapping("/sessions/{sessionId}/seats")
    public ApiResponse<List<SeatView>> seats(@PathVariable long sessionId, HttpServletRequest request) { return ok(seats.publicList(sessionId), request); }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return ApiResponse.ok(data, (String) request.getAttribute(RequestIdInterceptor.ATTRIBUTE));
    }
}
