package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ticketflow.common.AppException;
import com.ticketflow.common.AsyncStockGate;
import com.ticketflow.common.SeatEventPublisher;
import com.ticketflow.common.UserContext;
import com.ticketflow.entity.order.OrderEntity;
import com.ticketflow.entity.order.OrderItemEntity;
import com.ticketflow.entity.seat.SessionSeatEntity;
import com.ticketflow.entity.refund.RefundEntity;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.ticket.TicketEntity;
import com.ticketflow.mapper.refund.RefundMapper;
import com.ticketflow.service.IInventoryLogService;
import com.ticketflow.service.IOrderItemService;
import com.ticketflow.service.IOrderService;
import com.ticketflow.service.IQuotaService;
import com.ticketflow.service.ISeatCatalogService;
import com.ticketflow.service.ISessionService;
import com.ticketflow.service.ITicketService;
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

class RefundServiceImplTest {
    private final RefundMapper mapper = mock(RefundMapper.class);
    private final IOrderService orders = mock(IOrderService.class);
    private final IOrderItemService items = mock(IOrderItemService.class);
    private final ITicketService tickets = mock(ITicketService.class);
    private final ISessionService sessions = mock(ISessionService.class);
    private final IQuotaService quotas = mock(IQuotaService.class);
    private final ITierService tiers = mock(ITierService.class);
    private final IInventoryLogService inventoryLogs = mock(IInventoryLogService.class);
    private final ISeatCatalogService seats = mock(ISeatCatalogService.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final TransactionStatus transactionStatus = mock(TransactionStatus.class);

    @AfterEach
    void clearUser() { UserContext.clear(); }

    @Test
    void ticketWonCheckinRaceSoEntireRefundRequestRollsBack() {
        RefundServiceImpl service = service();
        OrderEntity order = order("TICKETED");
        SessionEntity session = new SessionEntity();
        session.setStartTime(LocalDateTime.now(Clock.systemUTC()).plusDays(2));
        OrderItemEntity item = new OrderItemEntity();
        item.setId(11L);
        TicketEntity ticket = new TicketEntity();
        ticket.setId(12L);
        when(orders.getById(8L)).thenReturn(order);
        when(sessions.getById(9L)).thenReturn(session);
        when(items.list(org.mockito.ArgumentMatchers.<Wrapper<OrderItemEntity>>any())).thenReturn(List.of(item));
        when(tickets.getOne(any(Wrapper.class))).thenReturn(ticket);
        when(orders.update(any(Wrapper.class))).thenReturn(true);
        when(tickets.update(any(Wrapper.class))).thenReturn(false);

        AppException error = assertThrows(AppException.class, () -> service.request(8L));

        assertEquals("TICKET_USED", error.code());
        verify(manager).rollback(transactionStatus);
        verify(mapper, never()).insert(any(RefundEntity.class));
        verifyNoInteractions(quotas, tiers, inventoryLogs);
    }

    @Test
    void alreadySettledRefundCannotReleaseStockAgain() {
        RefundServiceImpl service = service();
        RefundEntity candidate = new RefundEntity();
        candidate.setId(15L);
        candidate.setStatus("PROCESSING");
        RefundEntity settled = new RefundEntity();
        settled.setId(15L);
        settled.setStatus("SUCCEEDED");
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(candidate));
        when(mapper.selectById(15L)).thenReturn(settled);

        assertEquals(0, service.settlePending(100));

        verifyNoInteractions(orders, tickets, quotas, tiers, inventoryLogs);
    }

    @Test
    void failedRefundRestoresTicketAndOrderWithoutReleasingQuota() {
        RefundServiceImpl service = service();
        RefundEntity processing = new RefundEntity();
        processing.setId(15L);
        processing.setOrderId(8L);
        processing.setStatus("PROCESSING");
        processing.setPreviousOrderStatus("TICKETED");
        RefundEntity failed = new RefundEntity();
        failed.setId(15L);
        failed.setOrderId(8L);
        failed.setStatus("FAILED");
        OrderItemEntity item = new OrderItemEntity();
        item.setId(11L);
        TicketEntity ticket = new TicketEntity();
        ticket.setId(12L);
        when(mapper.selectById(15L)).thenReturn(processing, failed);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(orders.getById(8L)).thenReturn(order("REFUNDING"));
        when(items.list(org.mockito.ArgumentMatchers.<Wrapper<OrderItemEntity>>any())).thenReturn(List.of(item));
        when(tickets.getOne(any(Wrapper.class))).thenReturn(ticket);
        when(tickets.update(any(Wrapper.class))).thenReturn(true);
        when(orders.update(any(Wrapper.class))).thenReturn(true);

        assertEquals("FAILED", service.fail(15L, "SIMULATED_FAILURE").getStatus());

        verify(tickets).update(any(Wrapper.class));
        verify(orders).update(any(Wrapper.class));
        verifyNoInteractions(quotas, tiers, inventoryLogs);
    }

    @Test
    void settlementReleasesQuotaButDoesNotResellOutsideSaleWindow() {
        RefundServiceImpl service = service();
        RefundEntity processing = new RefundEntity();
        processing.setId(15L);
        processing.setOrderId(8L);
        processing.setStatus("PROCESSING");
        OrderItemEntity item = new OrderItemEntity();
        item.setId(11L);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(processing));
        when(mapper.selectById(15L)).thenReturn(processing);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(orders.getById(8L)).thenReturn(order("REFUNDING"));
        when(orders.update(any(Wrapper.class))).thenReturn(true);
        when(items.list(org.mockito.ArgumentMatchers.<Wrapper<OrderItemEntity>>any())).thenReturn(List.of(item));
        when(quotas.update(any(Wrapper.class))).thenReturn(true);

        assertEquals(1, service.settlePending(100));

        verify(quotas, times(1)).update(any(Wrapper.class));
        verify(tiers, never()).update(any(Wrapper.class));
        verifyNoInteractions(inventoryLogs);
    }

    @Test
    void assignedRefundMovesOnlyOwnedSoldSeatWithoutQuotaUpdate() {
        RefundServiceImpl service = service();
        RefundEntity processing = new RefundEntity();
        processing.setId(15L);
        processing.setOrderId(8L);
        processing.setStatus("PROCESSING");
        OrderEntity order = order("REFUNDING");
        order.setSaleMode("ASSIGNED");
        order.setTierId(null);
        OrderItemEntity item = new OrderItemEntity();
        item.setId(11L);
        SessionSeatEntity sold = new SessionSeatEntity();
        sold.setId(20L);
        sold.setStatus("SOLD");
        sold.setLockOrderId(8L);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(processing));
        when(mapper.selectById(15L)).thenReturn(processing);
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(1);
        when(orders.getById(8L)).thenReturn(order);
        when(orders.update(any(Wrapper.class))).thenReturn(true);
        when(items.list(org.mockito.ArgumentMatchers.<Wrapper<OrderItemEntity>>any())).thenReturn(List.of(item));
        when(seats.list(any(Wrapper.class))).thenReturn(List.of(sold));
        when(seats.update(any(Wrapper.class))).thenReturn(true);

        assertEquals(1, service.settlePending(100));

        verify(seats).update(any(Wrapper.class));
        verifyNoInteractions(quotas, tiers, inventoryLogs);
    }

    private RefundServiceImpl service() {
        RefundServiceImpl service = new RefundServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "orders", orders);
        ReflectionTestUtils.setField(service, "items", items);
        ReflectionTestUtils.setField(service, "tickets", tickets);
        ReflectionTestUtils.setField(service, "sessions", sessions);
        ReflectionTestUtils.setField(service, "quotas", quotas);
        ReflectionTestUtils.setField(service, "stockGate", mock(AsyncStockGate.class));
        ReflectionTestUtils.setField(service, "seats", seats);
        ReflectionTestUtils.setField(service, "seatEvents", mock(SeatEventPublisher.class));
        ReflectionTestUtils.setField(service, "tiers", tiers);
        ReflectionTestUtils.setField(service, "inventoryLogs", inventoryLogs);
        ReflectionTestUtils.setField(service, "transactions", new TransactionTemplate(manager));
        ReflectionTestUtils.setField(service, "cutoffHours", 24L);
        when(manager.getTransaction(any())).thenReturn(transactionStatus);
        UserContext.setUserId(7L);
        return service;
    }

    private OrderEntity order(String status) {
        OrderEntity order = new OrderEntity();
        order.setId(8L);
        order.setUserId(7L);
        order.setSessionId(9L);
        order.setTierId(10L);
        order.setSaleMode("GENERAL");
        order.setQuantity(1);
        order.setAmount(new BigDecimal("100.00"));
        order.setStatus(status);
        return order;
    }
}
