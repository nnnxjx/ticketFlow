package com.ticketflow.vo.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class AsyncOrderView {
    private final String requestId;
    private final String status;
    private final Long orderId;
    private final String failureCode;
}
