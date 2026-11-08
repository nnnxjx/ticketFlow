package com.ticketflow.mapper.order;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketflow.entity.order.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemEntity> {}
