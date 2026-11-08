package com.ticketflow.mapper.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketflow.entity.ticket.CheckinEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CheckinMapper extends BaseMapper<CheckinEntity> {}
