package com.ticketflow.entity.seat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@TableName("session_seat")
public class SessionSeatEntity {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long sessionId;
    private Long venueSeatId;
    private BigDecimal price;
    private String status;
    private Long lockOrderId;
    private LocalDateTime lockExpireAt;
    private Long version;
}
