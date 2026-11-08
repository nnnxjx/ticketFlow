package com.ticketflow.vo.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class PaymentView {
    private final long orderId;
    private final String paymentNo;
    private final BigDecimal amount;
    private final String status;
    private final LocalDateTime paidAt;
}
