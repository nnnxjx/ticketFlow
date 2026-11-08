package com.ticketflow.mapper.payment;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticketflow.entity.payment.PaymentEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentMapper extends BaseMapper<PaymentEntity> {}
