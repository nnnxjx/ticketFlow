package com.ticketflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ticketflow.dto.order.CreateGeneralOrderRequest;
import com.ticketflow.entity.order.OrderEntity;
import com.ticketflow.vo.order.OrderPageView;
import com.ticketflow.vo.order.OrderView;
import com.ticketflow.vo.payment.PaymentView;

public interface IOrderService extends IService<OrderEntity> {
    OrderView createGeneral(CreateGeneralOrderRequest request);
    OrderView detail(long orderId);
    OrderPageView mine(int page, int size);
    OrderView cancel(long orderId);
    PaymentView pay(long orderId);
    int closeExpired(int limit);
}
