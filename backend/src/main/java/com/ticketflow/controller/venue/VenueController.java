package com.ticketflow.controller.venue;

import com.ticketflow.common.ApiResponse;
import com.ticketflow.dto.venue.VenueRequest;
import com.ticketflow.dto.venue.VenueSeatRequest;
import com.ticketflow.interceptor.RequestIdInterceptor;
import com.ticketflow.service.IVenueService;
import com.ticketflow.vo.venue.VenueSeatView;
import com.ticketflow.vo.venue.VenueView;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/organizer")
@Tag(name = "场馆管理")
@SecurityRequirement(name = "jwtAuth")
public class VenueController {
    @Autowired private IVenueService venues;

    @PostMapping("/venues")
    public ApiResponse<VenueView> create(@Valid @RequestBody VenueRequest body, HttpServletRequest request) {
        return ok(venues.create(body), request);
    }
    @GetMapping("/venues")
    public ApiResponse<List<VenueView>> mine(HttpServletRequest request) { return ok(venues.mine(), request); }
    @GetMapping("/venues/{id}")
    public ApiResponse<VenueView> get(@PathVariable long id, HttpServletRequest request) { return ok(venues.getOwned(id), request); }
    @PutMapping("/venues/{id}")
    public ApiResponse<VenueView> update(@PathVariable long id, @Valid @RequestBody VenueRequest body, HttpServletRequest request) {
        return ok(venues.update(id, body), request);
    }
    @PostMapping("/venues/{venueId}/seats")
    public ApiResponse<VenueSeatView> addSeat(@PathVariable long venueId, @Valid @RequestBody VenueSeatRequest body, HttpServletRequest request) {
        return ok(venues.addSeat(venueId, body), request);
    }
    @GetMapping("/venues/{venueId}/seats")
    public ApiResponse<List<VenueSeatView>> seats(@PathVariable long venueId, HttpServletRequest request) {
        return ok(venues.seats(venueId), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        return ApiResponse.ok(data, (String) request.getAttribute(RequestIdInterceptor.ATTRIBUTE));
    }
}
