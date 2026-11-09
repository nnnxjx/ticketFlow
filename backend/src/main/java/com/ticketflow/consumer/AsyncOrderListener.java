package com.ticketflow.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.common.GeneralOrderMessage;
import com.ticketflow.config.AsyncOrderAmqpConfig;
import com.ticketflow.service.IAsyncOrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AsyncOrderListener {
    @Autowired private IAsyncOrderService orders;
    @Autowired private ObjectMapper json;

    // 业务拒单由 Service 释放预占；临时数据库异常交给 RabbitMQ 重新投递。
    @RabbitListener(queues = AsyncOrderAmqpConfig.QUEUE)
    public void handle(String body) throws JsonProcessingException {
        GeneralOrderMessage message = json.readValue(body, GeneralOrderMessage.class);
        orders.consume(message);
    }
}
