package com.ticketflow.vo.refund;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter @AllArgsConstructor
public class RefundView {
    private final long id;
    private final long orderId;
    private final String refundNo;
    private final BigDecimal amount;
    private final String status;
    private final String failureCode;
}
