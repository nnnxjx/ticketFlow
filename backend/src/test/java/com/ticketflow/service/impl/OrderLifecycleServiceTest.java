package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ticketflow.common.AppException;
import com.ticketflow.common.AsyncStockGate;
import com.ticketflow.common.Ids;
import com.ticketflow.common.UserContext;
import com.ticketflow.entity.event.EventEntity;
import com.ticketflow.entity.order.OrderEntity;
import com.ticketflow.entity.order.OrderItemEntity;
import com.ticketflow.entity.order.QuotaEntity;
import com.ticketflow.entity.refund.InventoryLogEntity;
import com.ticketflow.entity.seat.SessionSeatEntity;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.tier.TierEntity;
import com.ticketflow.mapper.order.OrderMapper;
import com.ticketflow.service.IOrderItemService;
import com.ticketflow.service.IEventService;
import com.ticketflow.service.IInventoryLogService;
import com.ticketflow.service.IPaymentService;
import com.ticketflow.service.IQuotaService;
import com.ticketflow.service.ISeatCatalogService;
import com.ticketflow.service.ISessionService;
import com.ticketflow.service.ITierService;
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

class OrderLifecycleServiceTest {
    private final OrderMapper mapper = mock(OrderMapper.class);
    private final IQuotaService quotas = mock(IQuotaService.class);
    private final ITierService tiers = mock(ITierService.class);
    private final ISessionService sessions = mock(ISessionService.class);
    private final IEventService events = mock(IEventService.class);
    private final IInventoryLogService inventoryLogs = mock(IInventoryLogService.class);
    private final Ids ids = mock(Ids.class);
    private final IOrderItemService items = mock(IOrderItemService.class);
    private final IPaymentService payments = mock(IPaymentService.class);
    private final ISeatCatalogService seats = mock(ISeatCatalogService.class);
    private final AsyncStockGate stockGate = mock(AsyncStockGate.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final TransactionStatus transactionStatus = mock(TransactionStatus.class);

    @AfterEach
    void clearUser() { UserContext.clear(); }

    @Test
    void losingCancellationDoesNotReleaseStockOrQuota() {
        OrderServiceImpl service = service();
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(order("PENDING_PAYMENT"), order("PAID"));
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(0);

        assertThrows(AppException.class, () -> service.cancel(8L));

        verifyNoInteractions(quotas, tiers, payments);
        verify(manager).rollback(transactionStatus);
    }

    @Test
    void successfulCancellationReleasesEachReservationOnce() {
        OrderServiceImpl service = service();
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(order("PENDING_PAYMENT"), order("CANCELLED"));
        OrderEntity cancelled = order("CANCELLED");
        cancelled.setRequestId("buy-1");
        when(mapper.selectById(8L)).thenReturn(cancelled);
        when(stockGate.isAsyncOrder(7L, "buy-1")).thenReturn(true);
        allowResale();
        when(inventoryLogs.save(any(InventoryLogEntity.class))).thenReturn(true);
        when(inventoryLogs.getOne(any(Wrapper.class))).thenReturn(new InventoryLogEntity());
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(quotas.update(any(Wrapper.class))).thenReturn(true);
        when(tiers.update(any(Wrapper.class))).thenReturn(true);
        when(items.list(org.mockito.ArgumentMatchers.<Wrapper<OrderItemEntity>>any())).thenReturn(List.of());

        assertEquals("CANCELLED", service.cancel(8L).getStatus());

        verify(quotas, times(1)).update(any(Wrapper.class));
        verify(tiers, times(1)).update(any(Wrapper.class));
        verify(inventoryLogs).save(any(InventoryLogEntity.class));
        verify(stockGate).credit(10L, 7L, 2, "CANCEL:8", true);
        verifyNoInteractions(payments);
        verify(manager).commit(transactionStatus);
    }

    @Test
    void cancellationAfterSaleWindowReleasesQuotaButNotStock() {
        OrderServiceImpl service = service();
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(order("PENDING_PAYMENT"), order("CANCELLED"));
        OrderEntity cancelled = order("CANCELLED");
        cancelled.setRequestId("buy-2");
        when(mapper.selectById(8L)).thenReturn(cancelled);
        when(stockGate.isAsyncOrder(7L, "buy-2")).thenReturn(true);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(quotas.update(any(Wrapper.class))).thenReturn(true);
        when(items.list(org.mockito.ArgumentMatchers.<Wrapper<OrderItemEntity>>any())).thenReturn(List.of());

        assertEquals("CANCELLED", service.cancel(8L).getStatus());

        verify(quotas).update(any(Wrapper.class));
        verify(tiers, never()).update(any(Wrapper.class));
        verify(inventoryLogs, never()).save(any(InventoryLogEntity.class));
        verify(stockGate).credit(10L, 7L, 2, "CANCEL:8", false);
    }

    @Test
    void expiredPaymentCannotWritePaymentRecord() {
        OrderServiceImpl service = service();
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(order("PENDING_PAYMENT"));
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(0);

        AppException error = assertThrows(AppException.class, () -> service.pay(8L));

        assertEquals("ORDER_EXPIRED", error.code());
        verifyNoInteractions(payments, quotas, tiers);
        verify(manager).commit(transactionStatus);
    }

    @Test
    void assignedPaymentRollsBackWhenOwnedSeatCannotBecomeSold() {
        OrderServiceImpl service = service();
        OrderEntity pending = order("PENDING_PAYMENT");
        pending.setSaleMode("ASSIGNED");
        pending.setTierId(null);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(pending);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        SessionSeatEntity locked = new SessionSeatEntity();
        locked.setId(20L);
        locked.setStatus("LOCKED");
        locked.setLockOrderId(8L);
        SessionSeatEntity second = new SessionSeatEntity();
        second.setId(21L);
        second.setStatus("LOCKED");
        second.setLockOrderId(8L);
        when(seats.list(any(Wrapper.class))).thenReturn(List.of(locked, second));
        when(seats.update(any(Wrapper.class))).thenReturn(false);

        assertThrows(AppException.class, () -> service.pay(8L));

        verify(seats).update(any(Wrapper.class));
        verifyNoInteractions(payments);
        verify(manager).rollback(transactionStatus);
    }

    @Test
    void assignedCancellationReleasesOnlyLockedSeats() {
        OrderServiceImpl service = service();
        OrderEntity pending = order("PENDING_PAYMENT");
        pending.setSaleMode("ASSIGNED");
        pending.setTierId(null);
        OrderEntity cancelled = order("CANCELLED");
        cancelled.setSaleMode("ASSIGNED");
        cancelled.setTierId(null);
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(pending, cancelled);
        when(mapper.selectById(8L)).thenReturn(cancelled);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        SessionSeatEntity locked = new SessionSeatEntity();
        locked.setId(20L);
        locked.setLockOrderId(8L);
        locked.setStatus("LOCKED");
        SessionSeatEntity second = new SessionSeatEntity();
        second.setId(21L);
        second.setLockOrderId(8L);
        second.setStatus("LOCKED");
        when(seats.list(any(Wrapper.class))).thenReturn(List.of(locked, second));
        when(seats.update(any(Wrapper.class))).thenReturn(true);
        when(items.list(org.mockito.ArgumentMatchers.<Wrapper<OrderItemEntity>>any())).thenReturn(List.of());

        assertEquals("CANCELLED", service.cancel(8L).getStatus());

        verify(seats, times(2)).update(any(Wrapper.class));
        verifyNoInteractions(quotas, tiers, stockGate);
        verify(manager).commit(transactionStatus);
    }

    private OrderServiceImpl service() {
        OrderServiceImpl service = new OrderServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "quotas", quotas);
        ReflectionTestUtils.setField(service, "stockGate", stockGate);
        ReflectionTestUtils.setField(service, "seats", seats);
        ReflectionTestUtils.setField(service, "sessions", sessions);
        ReflectionTestUtils.setField(service, "events", events);
        ReflectionTestUtils.setField(service, "inventoryLogs", inventoryLogs);
        ReflectionTestUtils.setField(service, "ids", ids);
        ReflectionTestUtils.setField(service, "tiers", tiers);
        ReflectionTestUtils.setField(service, "items", items);
        ReflectionTestUtils.setField(service, "payments", payments);
        ReflectionTestUtils.setField(service, "transactions", new TransactionTemplate(manager));
        when(manager.getTransaction(any())).thenReturn(transactionStatus);
        UserContext.setUserId(7L);
        return service;
    }

    private void allowResale() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        SessionEntity session = new SessionEntity();
        session.setEventId(11L);
        session.setStatus("ON_SALE");
        session.setStartTime(now.plusDays(2));
        session.setSaleStart(now.minusDays(1));
        session.setSaleEnd(now.plusDays(1));
        TierEntity tier = new TierEntity();
        tier.setStatus("ON_SALE");
        tier.setSaleStart(now.minusDays(1));
        tier.setSaleEnd(now.plusDays(1));
        EventEntity event = new EventEntity();
        event.setStatus("PUBLISHED");
        when(sessions.getById(9L)).thenReturn(session);
        when(tiers.getById(10L)).thenReturn(tier);
        when(events.getById(11L)).thenReturn(event);
    }

    private OrderEntity order(String status) {
        OrderEntity order = new OrderEntity();
        order.setId(8L);
        order.setUserId(7L);
        order.setSessionId(9L);
        order.setTierId(10L);
        order.setQuantity(2);
        order.setAmount(new BigDecimal("200.00"));
        order.setSaleMode("GENERAL");
        order.setStatus(status);
        return order;
    }
}
