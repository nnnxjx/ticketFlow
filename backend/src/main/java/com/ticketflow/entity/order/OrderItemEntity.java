package com.ticketflow.entity.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
@TableName("ticket_order_item")
public class OrderItemEntity {
    @TableId(type = IdType.INPUT) private Long id;
    private Long orderId;
    private Long tierId;
    private Long sessionSeatId;
    private String itemNameSnapshot;
    private BigDecimal priceSnapshot;
}
