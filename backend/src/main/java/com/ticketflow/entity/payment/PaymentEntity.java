package com.ticketflow.entity.payment;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@TableName("payment_record")
public class PaymentEntity {
    @TableId(type = IdType.INPUT) private Long id;
    private Long orderId;
    private String paymentNo;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paidAt;
}
