package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ticketflow.common.AppException;
import com.ticketflow.common.AsyncStockGate;
import com.ticketflow.common.GeneralOrderMessage;
import com.ticketflow.entity.order.OrderEntity;
import com.ticketflow.service.IOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AsyncOrderServiceImplTest {
    private final IOrderService orders = mock(IOrderService.class);
    private final AsyncStockGate stockGate = mock(AsyncStockGate.class);
    private AsyncOrderServiceImpl service;
    private final GeneralOrderMessage message = new GeneralOrderMessage(7L, "buy-1", 3L, 2);

    @BeforeEach
    void setUp() {
        service = new AsyncOrderServiceImpl();
        ReflectionTestUtils.setField(service, "orders", orders);
        ReflectionTestUtils.setField(service, "stockGate", stockGate);
    }

    @Test
    void duplicateDeliveryUsesCommittedOrderWithoutDeductingAgain() {
        OrderEntity existing = new OrderEntity();
        existing.setId(99L);
        existing.setRequestId("buy-1");
        existing.setSaleMode("GENERAL");
        existing.setTierId(3L);
        existing.setQuantity(2);
        when(orders.getOne(any(Wrapper.class))).thenReturn(existing);

        service.consume(message);

        verify(orders, never()).createAsync(anyLong(), anyString(), anyLong(), anyInt());
        verify(stockGate).confirm(7L, "buy-1", 3L);
    }

    @Test
    void rejectedDatabaseOrderReleasesRedisReservation() {
        when(stockGate.matchesToken(7L, "buy-1", 3L, 2)).thenReturn(true);
        when(orders.createAsync(7L, "buy-1", 3L, 2))
                .thenThrow(new AppException(HttpStatus.CONFLICT, "SOLD_OUT", "库存不足"));
        when(stockGate.release(7L, "buy-1", 3L)).thenReturn(true);

        service.consume(message);

        verify(stockGate).release(7L, "buy-1", 3L);
        verify(stockGate).markRejected(7L, "buy-1", "SOLD_OUT");
    }

    @Test
    void lateDeliveryAfterReleaseDoesNotCreateOrder() {
        service.consume(message);

        verify(orders, never()).createAsync(anyLong(), anyString(), anyLong(), anyInt());
        verify(stockGate, never()).confirm(anyLong(), anyString(), anyLong());
    }
}
