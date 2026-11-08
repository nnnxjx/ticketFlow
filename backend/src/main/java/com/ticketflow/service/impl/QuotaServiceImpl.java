package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.common.AppException;
import com.ticketflow.entity.order.QuotaEntity;
import com.ticketflow.mapper.order.QuotaMapper;
import com.ticketflow.service.IQuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class QuotaServiceImpl extends ServiceImpl<QuotaMapper, QuotaEntity> implements IQuotaService {
    @Autowired private QuotaMapper quotas;

    @Override
    public void reserve(long userId, long tierId, int quantity, int limit) {
        // 首次购票时补一行；已有记录由 (user_id,tier_id) 主键去重。
        quotas.ensure(userId, tierId);
        // 条件判断与增加数量由同一条 UPDATE 完成，并发请求不会基于旧值同时通过。
        if (!update(new LambdaUpdateWrapper<QuotaEntity>()
                .eq(QuotaEntity::getUserId, userId).eq(QuotaEntity::getTierId, tierId)
                .le(QuotaEntity::getActiveQty, limit - quantity)
                .setSql("active_qty = active_qty + {0}", quantity))) {
            throw new AppException(HttpStatus.CONFLICT, "LIMIT_EXCEEDED", "超过该票档每用户限购");
        }
    }
}
