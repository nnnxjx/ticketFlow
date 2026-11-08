package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ticketflow.common.AppException;
import com.ticketflow.common.CatalogAccess;
import com.ticketflow.common.UserContext;
import com.ticketflow.config.TicketCrypto;
import com.ticketflow.entity.order.OrderEntity;
import com.ticketflow.entity.order.OrderItemEntity;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.ticket.TicketEntity;
import com.ticketflow.common.Ids;
import com.ticketflow.mapper.ticket.TicketMapper;
import com.ticketflow.service.ICheckinService;
import com.ticketflow.service.IOrderItemService;
import com.ticketflow.service.IOrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TicketServiceImplTest {
    private final TicketMapper mapper = mock(TicketMapper.class);
    private final IOrderItemService items = mock(IOrderItemService.class);
    private final IOrderService orders = mock(IOrderService.class);
    private final ICheckinService checkins = mock(ICheckinService.class);
    private final CatalogAccess access = mock(CatalogAccess.class);
    private final TicketCrypto crypto = mock(TicketCrypto.class);
    private final Ids ids = mock(Ids.class);
    private final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    private final TransactionStatus transactionStatus = mock(TransactionStatus.class);

    @AfterEach
    void clearUser() { UserContext.clear(); }

    @Test
    void repeatedCheckinCannotWriteASecondRecord() {
        TicketServiceImpl service = service();
        fixture();
        when(mapper.update(isNull(), any(Wrapper.class))).thenReturn(0);

        AppException error = assertThrows(AppException.class, () -> service.checkin("secret"));

        assertEquals("TICKET_NOT_USABLE", error.code());
        verifyNoInteractions(checkins);
        verify(manager).rollback(transactionStatus);
    }

    @Test
    void otherOrganizerCannotChangeTicketState() {
        TicketServiceImpl service = service();
        fixture();
        when(access.sessionOwned(9L)).thenThrow(CatalogAccess.notFound());

        assertThrows(AppException.class, () -> service.checkin("secret"));

        verify(mapper, never()).update(any(), any());
        verifyNoInteractions(checkins);
    }

    @Test
    void checkinOutsideWindowDoesNotMarkTicketUsed() {
        TicketServiceImpl service = service();
        fixture();
        SessionEntity closed = new SessionEntity();
        closed.setCheckinStart(LocalDateTime.now(Clock.systemUTC()).plusHours(1));
        closed.setCheckinEnd(LocalDateTime.now(Clock.systemUTC()).plusHours(2));
        when(access.sessionOwned(9L)).thenReturn(closed);

        AppException error = assertThrows(AppException.class, () -> service.checkin("secret"));

        assertEquals("CHECKIN_CLOSED", error.code());
        verify(mapper, never()).update(any(), any());
        verifyNoInteractions(checkins);
    }

    @Test
    void existingTicketIsNotReissued() {
        TicketServiceImpl service = service();
        OrderEntity order = new OrderEntity();
        order.setId(8L);
        order.setQuantity(1);
        order.setStatus("PAID");
        OrderItemEntity item = new OrderItemEntity();
        item.setId(11L);
        item.setOrderId(8L);
        TicketEntity ticket = new TicketEntity();
        ticket.setOrderItemId(11L);
        when(orders.list(org.mockito.ArgumentMatchers.<Wrapper<OrderEntity>>any())).thenReturn(List.of(order));
        when(orders.getById(8L)).thenReturn(order);
        when(items.list(org.mockito.ArgumentMatchers.<Wrapper<OrderItemEntity>>any())).thenReturn(List.of(item));
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(ticket);
        when(orders.update(any(Wrapper.class))).thenReturn(true);

        assertEquals(1, service.issuePaid(100));
        verify(mapper, never()).insert(any(TicketEntity.class));
        verify(orders).update(any(Wrapper.class));
    }

    private TicketServiceImpl service() {
        TicketServiceImpl service = new TicketServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", mapper);
        ReflectionTestUtils.setField(service, "items", items);
        ReflectionTestUtils.setField(service, "orders", orders);
        ReflectionTestUtils.setField(service, "checkins", checkins);
        ReflectionTestUtils.setField(service, "access", access);
        ReflectionTestUtils.setField(service, "crypto", crypto);
        ReflectionTestUtils.setField(service, "ids", ids);
        ReflectionTestUtils.setField(service, "transactions", new TransactionTemplate(manager));
        when(manager.getTransaction(any())).thenReturn(transactionStatus);
        when(crypto.hash("secret")).thenReturn(new byte[32]);
        UserContext.setUserId(7L);
        return service;
    }

    private void fixture() {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(10L);
        ticket.setOrderItemId(11L);
        ticket.setStatus("UNUSED");
        OrderItemEntity item = new OrderItemEntity();
        item.setId(11L);
        item.setOrderId(8L);
        OrderEntity order = new OrderEntity();
        order.setId(8L);
        order.setSessionId(9L);
        order.setStatus("TICKETED");
        SessionEntity session = new SessionEntity();
        session.setCheckinStart(LocalDateTime.now(Clock.systemUTC()).minusHours(1));
        session.setCheckinEnd(LocalDateTime.now(Clock.systemUTC()).plusHours(1));
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(ticket);
        when(items.getById(11L)).thenReturn(item);
        when(orders.getById(8L)).thenReturn(order);
        when(access.sessionOwned(9L)).thenReturn(session);
    }
}
