package com.ticketflow.entity.ticket;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@TableName("checkin_record")
public class CheckinEntity {
    @TableId(type = IdType.INPUT) private Long id;
    private Long ticketId;
    private Long operatorId;
    private LocalDateTime checkedAt;
}
