package com.ticketflow.mapper.event;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketflow.entity.event.EventEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventMapper extends BaseMapper<EventEntity> {}
