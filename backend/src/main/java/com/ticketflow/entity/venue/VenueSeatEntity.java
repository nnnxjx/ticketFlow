package com.ticketflow.entity.venue;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("venue_seat")
public class VenueSeatEntity {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long venueId;
    private String area;
    private String rowNo;
    private String seatNo;
}
