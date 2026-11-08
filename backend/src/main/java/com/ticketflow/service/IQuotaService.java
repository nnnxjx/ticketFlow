package com.ticketflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ticketflow.entity.order.QuotaEntity;

public interface IQuotaService extends IService<QuotaEntity> {
    void reserve(long userId, long tierId, int quantity, int limit);
}
