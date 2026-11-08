package com.ticketflow.vo.ticket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class TicketView {
    private final long id;
    private final long orderItemId;
    private final String status;
    private final LocalDateTime issuedAt;
    private final String token;
}
