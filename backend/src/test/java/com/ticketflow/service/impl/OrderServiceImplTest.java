package com.ticketflow.service.impl;

import com.ticketflow.common.AppException;
import com.ticketflow.common.Ids;
import com.ticketflow.common.UserContext;
import com.ticketflow.dto.order.CreateGeneralOrderRequest;
import com.ticketflow.entity.event.EventEntity;
import com.ticketflow.entity.order.OrderEntity;
import com.ticketflow.entity.order.OrderItemEntity;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.tier.TierEntity;
import com.ticketflow.mapper.order.OrderMapper;
import com.ticketflow.service.IEventService;
import com.ticketflow.service.IOrderItemService;
import com.ticketflow.service.IQuotaService;
import com.ticketflow.service.ISessionService;
import com.ticketflow.service.ITierService;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {
    private final OrderMapper orders = mock(OrderMapper.class);
    private final IOrderItemService items = mock(IOrderItemService.class);
    private final ITierService tiers = mock(ITierService.class);
    private final ISessionService sessions = mock(ISessionService.class);
    private final IEventService events = mock(IEventService.class);
    private final IQuotaService quotas = mock(IQuotaService.class);
    private final Ids ids = mock(Ids.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final TransactionStatus status = mock(TransactionStatus.class);
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", orders);
        ReflectionTestUtils.setField(service, "items", items);
        ReflectionTestUtils.setField(service, "tiers", tiers);
        ReflectionTestUtils.setField(service, "sessions", sessions);
        ReflectionTestUtils.setField(service, "events", events);
        ReflectionTestUtils.setField(service, "quotas", quotas);
        ReflectionTestUtils.setField(service, "ids", ids);
        ReflectionTestUtils.setField(service, "transactions", new TransactionTemplate(manager));
        ReflectionTestUtils.setField(service, "timeoutMinutes", 15L);
        UserContext.setUserId(7L);
        when(manager.getTransaction(any())).thenReturn(status);
    }

    @AfterEach
    void cleanUp() { UserContext.clear(); }

    @Test
    void repeatWithDifferentQuantityIsRejectedBeforeStockChange() {
        OrderEntity existing = new OrderEntity();
        existing.setId(99L);
        existing.setUserId(7L);
        existing.setRequestId("retry-1");
        existing.setSaleMode("GENERAL");
        existing.setTierId(3L);
        existing.setQuantity(1);
        when(orders.selectOne(any())).thenReturn(existing);

        CreateGeneralOrderRequest request = request("retry-1", 3L, 2);
        AppException error = assertThrows(AppException.class, () -> service.createGeneral(request));
        assertEquals("REQUEST_ID_CONFLICT", error.code());
        verifyNoInteractions(tiers, quotas);
        verifyNoInteractions(manager);
    }

    @Test
    void repeatWithSameParametersReturnsOriginalOrder() {
        OrderEntity existing = new OrderEntity();
        existing.setId(99L);
        existing.setUserId(7L);
        existing.setSessionId(4L);
        existing.setRequestId("retry-2");
        existing.setSaleMode("GENERAL");
        existing.setTierId(3L);
        existing.setQuantity(2);
        existing.setAmount(new BigDecimal("200.00"));
        existing.setStatus("PENDING_PAYMENT");
        when(orders.selectOne(any())).thenReturn(existing);
        when(items.list(org.mockito.ArgumentMatchers.<Wrapper<OrderItemEntity>>any())).thenReturn(List.of());

        assertEquals(99L, service.createGeneral(request("retry-2", 3L, 2)).getId());
        verifyNoInteractions(tiers, quotas, manager);
    }

    @Test
    void quotaFailureRollsBackInsertedOrderAndSkipsStockChange() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        TierEntity tier = new TierEntity();
        tier.setId(3L);
        tier.setSessionId(4L);
        tier.setStatus("ON_SALE");
        tier.setSaleStart(now.minusHours(1));
        tier.setSaleEnd(now.plusHours(1));
        tier.setPerUserLimit(4);
        tier.setPrice(new BigDecimal("100.00"));
        SessionEntity session = new SessionEntity();
        session.setId(4L);
        session.setEventId(5L);
        session.setSaleMode("GENERAL");
        session.setStatus("ON_SALE");
        session.setSaleStart(now.minusHours(1));
        session.setSaleEnd(now.plusHours(1));
        EventEntity event = new EventEntity();
        event.setStatus("PUBLISHED");
        when(tiers.getById(3L)).thenReturn(tier);
        when(sessions.getById(4L)).thenReturn(session);
        when(events.getById(5L)).thenReturn(event);
        when(ids.next()).thenReturn(90L);
        when(orders.insert(any(OrderEntity.class))).thenReturn(1);
        doThrow(new AppException(HttpStatus.CONFLICT, "LIMIT_EXCEEDED", "超出限购"))
                .when(quotas).reserve(7L, 3L, 2, 4);

        AppException error = assertThrows(AppException.class,
                () -> service.createGeneral(request("quota-1", 3L, 2)));

        assertEquals("LIMIT_EXCEEDED", error.code());
        verify(manager).rollback(status);
        verify(tiers, never()).update(any());
        verifyNoInteractions(items);
    }

    @Test
    void anotherUsersOrderIsNotVisible() {
        AppException error = assertThrows(AppException.class, () -> service.detail(99L));
        assertEquals("NOT_FOUND", error.code());
        verify(orders).selectOne(any());
        verifyNoInteractions(items);
    }

    private CreateGeneralOrderRequest request(String requestId, long tierId, int quantity) {
        CreateGeneralOrderRequest request = new CreateGeneralOrderRequest();
        request.setRequestId(requestId);
        request.setTierId(tierId);
        request.setQuantity(quantity);
        return request;
    }
}
