package com.ticketflow.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketflow.entity.auth.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {}
