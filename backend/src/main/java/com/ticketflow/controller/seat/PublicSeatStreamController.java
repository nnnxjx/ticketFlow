package com.ticketflow.controller.seat;

import com.ticketflow.common.SeatEventPublisher;
import com.ticketflow.service.ISeatCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/sessions")
@Tag(name = "公开座位状态")
public class PublicSeatStreamController {
    @Autowired private ISeatCatalogService seats;
    @Autowired private SeatEventPublisher events;

    @GetMapping(value = "/{sessionId}/seats/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅场次座位变化；连接建立或重连后仍需读取 REST 快照")
    public SseEmitter stream(@PathVariable long sessionId) {
        seats.publicList(sessionId);
        return events.subscribe(sessionId);
    }
}
