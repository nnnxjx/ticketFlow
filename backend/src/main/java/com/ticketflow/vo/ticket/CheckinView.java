package com.ticketflow.vo.ticket;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class CheckinView {
    private final long ticketId;
    private final long operatorId;
    private final LocalDateTime checkedAt;
}
