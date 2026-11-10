package com.ticketflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ticketflow.dto.order.CreateGeneralOrderRequest;
import com.ticketflow.dto.order.CreateAssignedOrderRequest;
import com.ticketflow.entity.order.OrderEntity;
import com.ticketflow.vo.order.OrderPageView;
import com.ticketflow.vo.order.OrderView;
import com.ticketflow.vo.payment.PaymentView;

public interface IOrderService extends IService<OrderEntity> {
    // 旧同步下单入口保留作参考；票档购买统一调用异步受理接口。
    // OrderView createGeneral(CreateGeneralOrderRequest request);
    long createAsync(long userId, String requestId, long tierId, int quantity);
    OrderView createAssigned(CreateAssignedOrderRequest request);
    OrderView detail(long orderId);
    OrderPageView mine(int page, int size);
    OrderPageView organizerOrders(Long eventId, int page, int size);
    OrderView cancel(long orderId);
    PaymentView pay(long orderId);
    int closeExpired(int limit);
}
