package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.entity.ticket.CheckinEntity;
import com.ticketflow.mapper.ticket.CheckinMapper;
import com.ticketflow.service.ICheckinService;
import org.springframework.stereotype.Service;

@Service
public class CheckinServiceImpl extends ServiceImpl<CheckinMapper, CheckinEntity> implements ICheckinService {}
