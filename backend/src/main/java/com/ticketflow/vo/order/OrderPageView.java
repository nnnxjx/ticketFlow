package com.ticketflow.vo.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter @AllArgsConstructor
public class OrderPageView {
    private final int page;
    private final int size;
    private final long total;
    private final List<OrderView> orders;
}
