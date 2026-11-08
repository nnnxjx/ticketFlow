package com.ticketflow.entity.refund;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
@TableName("refund_record")
public class RefundEntity {
    @TableId(type = IdType.INPUT) private Long id;
    private Long orderId;
    private String refundNo;
    private BigDecimal amount;
    private String previousOrderStatus;
    private String status;
    private String failureCode;
}
