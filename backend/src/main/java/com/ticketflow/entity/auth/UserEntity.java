package com.ticketflow.entity.auth;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@TableName("users")
@Getter
@Setter
public class UserEntity {
    @TableId(type = IdType.INPUT)
    private Long id;
    private String phone;
    private String passwordHash;
    private String status;

}
