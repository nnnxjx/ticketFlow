package com.ticketflow.entity.venue;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("venue")
public class VenueEntity {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long organizerId;
    private String name;
    private String address;
}
