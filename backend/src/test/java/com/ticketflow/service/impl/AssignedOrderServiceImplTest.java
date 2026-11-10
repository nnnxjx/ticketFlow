package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ticketflow.common.AppException;
import com.ticketflow.common.Ids;
import com.ticketflow.common.SeatEventPublisher;
import com.ticketflow.common.UserContext;
import com.ticketflow.dto.order.CreateAssignedOrderRequest;
import com.ticketflow.entity.event.EventEntity;
import com.ticketflow.entity.order.OrderEntity;
import com.ticketflow.entity.seat.SessionSeatEntity;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.venue.VenueSeatEntity;
import com.ticketflow.mapper.order.OrderMapper;
import com.ticketflow.mapper.venue.VenueSeatMapper;
import com.ticketflow.service.IEventService;
import com.ticketflow.service.IOrderItemService;
import com.ticketflow.service.ISeatCatalogService;
import com.ticketflow.service.ISessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssignedOrderServiceImplTest {
    private final OrderMapper mapper = mock(OrderMapper.class);
    private final ISessionService sessions = mock(ISessionService.class);
    private final IEventService events = mock(IEventService.class);
    private final ISeatCatalogService seats = mock(ISeatCatalogService.class);
    private final VenueSeatMapper venueSeats = mock(VenueSeatMapper.class);
    private final IOrderItemService items = mock(IOrderItemService.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final TransactionStatus transaction = mock(TransactionStatus.class);

    @AfterEach void clearUser() { UserContext.clear(); }

    @Test
    void secondSeatConflictRollsBackFirstLockAndOrder() {
        OrderServiceImpl service = service();
        saleOpen();
        when(seats.getById(20L)).thenReturn(seat(20L));
        when(seats.getById(30L)).thenReturn(seat(30L));
        when(venueSeats.selectById(120L)).thenReturn(template(120L));
        when(venueSeats.selectById(130L)).thenReturn(template(130L));
        when(seats.update(any(Wrapper.class))).thenReturn(true, false);

        AppException error = assertThrows(AppException.class,
                () -> service.createAssigned(request(List.of(30L, 20L))));

        assertEquals("SEAT_UNAVAILABLE", error.code());
        verify(seats, times(2)).update(any(Wrapper.class));
        verify(manager).rollback(transaction);
        verify(mapper, never()).insert(any(OrderEntity.class));
        verifyNoInteractions(items);
    }

    @Test
    void repeatedSeatIdIsRejectedBeforeAnyDatabaseMutation() {
        OrderServiceImpl service = service();

        assertThrows(AppException.class, () -> service.createAssigned(request(List.of(20L, 20L))));

        verifyNoInteractions(seats, manager);
    }

    private OrderServiceImpl service() {
        OrderServiceImpl service = new OrderServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "sessions", sessions);
        ReflectionTestUtils.setField(service, "events", events);
        ReflectionTestUtils.setField(service, "seats", seats);
        ReflectionTestUtils.setField(service, "seatEvents", mock(SeatEventPublisher.class));
        ReflectionTestUtils.setField(service, "venueSeats", venueSeats);
        ReflectionTestUtils.setField(service, "items", items);
        ReflectionTestUtils.setField(service, "ids", mock(Ids.class));
        ReflectionTestUtils.setField(service, "transactions", new TransactionTemplate(manager));
        ReflectionTestUtils.setField(service, "timeoutMinutes", 15L);
        ReflectionTestUtils.setField(service, "maxSeatsPerOrder", 8);
        when(manager.getTransaction(any())).thenReturn(transaction);
        UserContext.setUserId(7L);
        return service;
    }

    private void saleOpen() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        SessionEntity session = new SessionEntity();
        session.setId(9L);
        session.setEventId(11L);
        session.setVenueId(12L);
        session.setSaleMode("ASSIGNED");
        session.setStatus("ON_SALE");
        session.setSaleStart(now.minusDays(1));
        session.setSaleEnd(now.plusDays(1));
        session.setStartTime(now.plusDays(2));
        EventEntity event = new EventEntity();
        event.setStatus("PUBLISHED");
        when(sessions.getOne(any(Wrapper.class))).thenReturn(session);
        when(events.getById(11L)).thenReturn(event);
    }

    private SessionSeatEntity seat(long id) {
        SessionSeatEntity seat = new SessionSeatEntity();
        seat.setId(id);
        seat.setSessionId(9L);
        seat.setVenueSeatId(id + 100);
        seat.setStatus("FREE");
        seat.setPrice(new BigDecimal("100.00"));
        return seat;
    }

    private VenueSeatEntity template(long id) {
        VenueSeatEntity seat = new VenueSeatEntity();
        seat.setId(id);
        seat.setVenueId(12L);
        seat.setArea("A");
        seat.setRowNo("1");
        seat.setSeatNo("1");
        return seat;
    }

    private CreateAssignedOrderRequest request(List<Long> seatIds) {
        CreateAssignedOrderRequest request = new CreateAssignedOrderRequest();
        request.setRequestId("seat-buy-1");
        request.setSessionId(9L);
        request.setSeatIds(seatIds);
        return request;
    }
}
