package com.ticketflow.entity.tier;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@TableName("ticket_tier")
public class TierEntity {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long sessionId;
    private String name;
    private BigDecimal price;
    private Integer totalStock;
    private Integer availableStock;
    private Integer perUserLimit;
    private LocalDateTime saleStart;
    private LocalDateTime saleEnd;
    private String status;
}
