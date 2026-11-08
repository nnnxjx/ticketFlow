package com.ticketflow.mapper.session;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketflow.entity.session.SessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SessionMapper extends BaseMapper<SessionEntity> {
    @Select("SELECT COUNT(*) FROM ticket_order WHERE session_id = #{sessionId}")
    long countOrders(@Param("sessionId") long sessionId);
}
