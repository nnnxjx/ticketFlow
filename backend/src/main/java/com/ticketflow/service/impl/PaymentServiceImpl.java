package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.entity.payment.PaymentEntity;
import com.ticketflow.mapper.payment.PaymentMapper;
import com.ticketflow.service.IPaymentService;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl extends ServiceImpl<PaymentMapper, PaymentEntity> implements IPaymentService {}
