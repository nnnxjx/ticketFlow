package com.ticketflow.entity.event;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("event")
public class EventEntity {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long organizerId;
    private String title;
    private String category;
    private String description;
    private String status;
}
