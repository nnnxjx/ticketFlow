package com.ticketflow.service;

import com.ticketflow.dto.order.CreateGeneralOrderRequest;
import com.ticketflow.common.GeneralOrderMessage;
import com.ticketflow.vo.order.AsyncOrderView;

public interface IAsyncOrderService {
    AsyncOrderView submit(CreateGeneralOrderRequest request);
    AsyncOrderView status(String requestId);
    void consume(GeneralOrderMessage message);
}
