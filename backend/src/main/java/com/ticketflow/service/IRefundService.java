package com.ticketflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ticketflow.entity.refund.RefundEntity;
import com.ticketflow.vo.refund.RefundView;

public interface IRefundService extends IService<RefundEntity> {
    RefundView request(long orderId);
    RefundView mine(long orderId);
    int settlePending(int limit);
    RefundView fail(long refundId, String failureCode);
}
