package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.entity.refund.InventoryLogEntity;
import com.ticketflow.mapper.refund.InventoryLogMapper;
import com.ticketflow.service.IInventoryLogService;
import org.springframework.stereotype.Service;

@Service
public class InventoryLogServiceImpl extends ServiceImpl<InventoryLogMapper, InventoryLogEntity> implements IInventoryLogService {}
