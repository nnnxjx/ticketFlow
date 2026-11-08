package com.ticketflow.entity.session;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@TableName("event_session")
public class SessionEntity {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long eventId;
    private Long venueId;
    private String saleMode;
    private LocalDateTime saleStart;
    private LocalDateTime saleEnd;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime checkinStart;
    private LocalDateTime checkinEnd;
    private String status;
}
