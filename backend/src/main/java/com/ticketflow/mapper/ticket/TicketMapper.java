package com.ticketflow.mapper.ticket;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketflow.entity.ticket.TicketEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TicketMapper extends BaseMapper<TicketEntity> {}
