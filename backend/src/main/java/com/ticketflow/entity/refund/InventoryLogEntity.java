package com.ticketflow.entity.refund;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("inventory_log")
public class InventoryLogEntity {
    @TableId(type = IdType.INPUT) private Long id;
    private String operationId;
    private Long orderId;
    private Long tierId;
    private Long sessionSeatId;
    private String operation;
    private Integer quantity;
}
