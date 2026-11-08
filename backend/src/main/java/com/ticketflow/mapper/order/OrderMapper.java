package com.ticketflow.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketflow.entity.order.OrderEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {}
