package com.ticketflow.vo.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter @AllArgsConstructor
public class OrderItemView {
    private final long id;
    private final String name;
    private final BigDecimal price;
    private final Long sessionSeatId;

    public OrderItemView(long id, String name, BigDecimal price) {
        this(id, name, price, null);
    }
}
