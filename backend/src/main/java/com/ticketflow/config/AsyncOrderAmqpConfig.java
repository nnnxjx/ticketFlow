package com.ticketflow.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AsyncOrderAmqpConfig {
    public static final String EXCHANGE = "ticketflow.orders";
    public static final String QUEUE = "ticketflow.general-orders";
    public static final String ROUTING_KEY = "general-order";

    @Bean public DirectExchange orderExchange() { return new DirectExchange(EXCHANGE, true, false); }
    @Bean public Queue generalOrderQueue() { return new Queue(QUEUE, true); }
    @Bean public Binding generalOrderBinding(Queue generalOrderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(generalOrderQueue).to(orderExchange).with(ROUTING_KEY);
    }
}
