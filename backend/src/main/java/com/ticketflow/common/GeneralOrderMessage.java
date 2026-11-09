package com.ticketflow.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** RabbitMQ 只传建单所需的身份和请求参数；价格仍由数据库计算。 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class GeneralOrderMessage {
    private Long userId;
    private String requestId;
    private Long tierId;
    private Integer quantity;
}
