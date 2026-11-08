package com.ticketflow.entity.organizer;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@TableName("organizer")
@Getter
@Setter
public class OrganizerEntity {
    @TableId(type = IdType.INPUT)
    private Long id;
    private Long ownerUserId;
    private String name;
    private String status;
    private LocalDateTime createdAt;

}
