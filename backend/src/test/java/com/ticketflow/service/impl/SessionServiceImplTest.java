package com.ticketflow.service.impl;

import com.ticketflow.common.AppException;
import com.ticketflow.common.CatalogAccess;
import com.ticketflow.common.Ids;
import com.ticketflow.dto.session.SessionRequest;
import com.ticketflow.entity.event.EventEntity;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.tier.TierEntity;
import com.ticketflow.entity.seat.SessionSeatEntity;
import com.ticketflow.mapper.session.SessionMapper;
import com.ticketflow.mapper.tier.TierMapper;
import com.ticketflow.mapper.seat.SessionSeatMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionServiceImplTest {
    private final CatalogAccess access = mock(CatalogAccess.class);
    private final SessionMapper mapper = mock(SessionMapper.class);
    private final TierMapper tiers = mock(TierMapper.class);
    private final SessionSeatMapper seats = mock(SessionSeatMapper.class);
    private final Ids ids = mock(Ids.class);
    private SessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SessionServiceImpl();
        ReflectionTestUtils.setField(service, "access", access);
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "tiers", tiers);
        ReflectionTestUtils.setField(service, "seats", seats);
        ReflectionTestUtils.setField(service, "ids", ids);
        EventEntity event = new EventEntity();
        event.setId(10L);
        event.setOrganizerId(20L);
        event.setStatus("DRAFT");
        when(access.eventOwned(10L)).thenReturn(event);
    }

    @Test
    void everySessionRequiresVenue() {
        SessionRequest request = validRequest();
        AppException ex = assertThrows(AppException.class, () -> service.create(10L, request));
        assertEquals("INVALID_ARGUMENT", ex.code());
        verify(mapper, never()).insert((SessionEntity) any(SessionEntity.class));
    }

    @Test
    void saleEndCannotFollowPerformanceStart() {
        SessionRequest request = validRequest();
        request.setSaleEnd(request.getStartTime().plusMinutes(1));
        AppException ex = assertThrows(AppException.class, () -> service.create(10L, request));
        assertEquals("INVALID_ARGUMENT", ex.code());
    }

    @Test
    void deletingDraftSessionAlsoDeletesTiersAndSeats() {
        SessionEntity session = new SessionEntity();
        session.setId(88L);
        session.setStatus("DRAFT");
        session.setSaleMode("GENERAL");
        when(access.sessionOwned(88L)).thenReturn(session);
        when(mapper.countOrders(88L)).thenReturn(0L);
        when(mapper.delete((Wrapper<SessionEntity>) any(Wrapper.class))).thenReturn(1);

        service.delete(88L);

        verify(tiers).delete((Wrapper<TierEntity>) any(Wrapper.class));
        verify(seats).delete((Wrapper<SessionSeatEntity>) any(Wrapper.class));
    }

    private SessionRequest validRequest() {
        LocalDateTime day = LocalDateTime.of(2027, 1, 1, 0, 0);
        SessionRequest request = new SessionRequest();
        request.setSaleMode("GENERAL");
        request.setSaleStart(day);
        request.setSaleEnd(day.plusDays(1));
        request.setStartTime(day.plusDays(2));
        request.setEndTime(day.plusDays(2).plusHours(3));
        request.setCheckinStart(day.plusDays(2).minusHours(1));
        request.setCheckinEnd(day.plusDays(2).plusHours(2));
        return request;
    }
}
