package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.entity.order.OrderItemEntity;
import com.ticketflow.mapper.order.OrderItemMapper;
import com.ticketflow.service.IOrderItemService;
import org.springframework.stereotype.Service;

@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItemEntity> implements IOrderItemService {}
