package com.ticketflow.entity.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@TableName("ticket_order")
public class OrderEntity {
    @TableId(type = IdType.INPUT) private Long id;
    private Long userId;
    private Long sessionId;
    private Long tierId;
    private String requestId;
    private String saleMode;
    private Integer quantity;
    private BigDecimal amount;
    private String status;
    private LocalDateTime expireAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
