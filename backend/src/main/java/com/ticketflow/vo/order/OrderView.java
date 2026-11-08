package com.ticketflow.vo.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @AllArgsConstructor
public class OrderView {
    private final long id;
    private final long sessionId;
    private final Long tierId;
    private final String requestId;
    private final String saleMode;
    private final int quantity;
    private final BigDecimal amount;
    private final String status;
    private final LocalDateTime expireAt;
    private final LocalDateTime createdAt;
    private final List<OrderItemView> items;
}
