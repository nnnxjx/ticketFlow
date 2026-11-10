package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ticketflow.common.AppException;
import com.ticketflow.common.AsyncStockGate;
import com.ticketflow.common.CatalogAccess;
import com.ticketflow.dto.tier.TierRequest;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.tier.TierEntity;
import com.ticketflow.mapper.session.SessionMapper;
import com.ticketflow.mapper.tier.TierMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TierAsyncSwitchTest {
    private final TierMapper mapper = mock(TierMapper.class);
    private final SessionMapper sessions = mock(SessionMapper.class);
    private final CatalogAccess access = mock(CatalogAccess.class);
    private final AsyncStockGate gate = mock(AsyncStockGate.class);
    private final TierServiceImpl service = new TierServiceImpl();

    private void wire() {
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "sessions", sessions);
        ReflectionTestUtils.setField(service, "access", access);
        ReflectionTestUtils.setField(service, "stockGate", gate);
    }

    @Test
    void activationUsesLockedCurrentStockBeforeSelling() {
        wire();
        TierEntity stale = tier(10, "DRAFT");
        TierEntity current = tier(20, "DRAFT");
        when(access.tierOwned(1L)).thenReturn(stale);
        when(sessions.selectOne(any(Wrapper.class))).thenReturn(session());
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(current);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(gate.ready(1L)).thenReturn(true);

        service.activate(1L);

        var order = inOrder(mapper, gate);
        order.verify(gate).initialize(current);
        order.verify(mapper).update(isNull(), any(Wrapper.class));
        assertEquals(20, current.getAvailableStock());
        assertEquals("ON_SALE", current.getStatus());
    }

    @Test
    void stoppedTierCanResumeWithoutResettingRedisStock() {
        wire();
        when(access.tierOwned(1L)).thenReturn(tier(20, "STOPPED"));
        when(sessions.selectOne(any(Wrapper.class))).thenReturn(session());
        TierEntity current = tier(12, "STOPPED");
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(current);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(gate.ready(1L)).thenReturn(true);

        service.activate(1L);

        verify(gate, never()).initialize(any());
        verify(mapper).update(isNull(), any(Wrapper.class));
        assertEquals(12, current.getAvailableStock());
        assertEquals("ON_SALE", current.getStatus());
    }

    @Test
    void stoppedTierCannotResumeWhenRedisStockIsUnavailable() {
        wire();
        when(access.tierOwned(1L)).thenReturn(tier(20, "STOPPED"));
        when(sessions.selectOne(any(Wrapper.class))).thenReturn(session());
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(tier(12, "STOPPED"));

        assertThrows(AppException.class, () -> service.activate(1L));
        verify(gate, never()).initialize(any());
        verify(mapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void editRejectsStatusChangedAfterInitialRead() {
        wire();
        when(access.tierOwned(1L)).thenReturn(tier(10, "DRAFT"));
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(tier(10, "ON_SALE"));
        when(access.sessionOwned(2L)).thenReturn(session());

        assertThrows(AppException.class, () -> service.update(1L, new TierRequest()));
        verify(mapper, never()).update(any(), any(Wrapper.class));
    }

    private SessionEntity session() {
        SessionEntity session = new SessionEntity();
        session.setId(2L);
        session.setSaleMode("GENERAL");
        session.setStatus("DRAFT");
        return session;
    }

    private TierEntity tier(int stock, String status) {
        TierEntity tier = new TierEntity();
        tier.setId(1L);
        tier.setSessionId(2L);
        tier.setName("VIP");
        tier.setPrice(new BigDecimal("100.00"));
        tier.setTotalStock(stock);
        tier.setAvailableStock(stock);
        tier.setPerUserLimit(4);
        tier.setSaleStart(LocalDateTime.of(2027, 1, 1, 0, 0));
        tier.setSaleEnd(LocalDateTime.of(2027, 1, 2, 0, 0));
        tier.setStatus(status);
        return tier;
    }
}
