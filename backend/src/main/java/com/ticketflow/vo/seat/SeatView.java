package com.ticketflow.vo.seat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter @AllArgsConstructor
public class SeatView {
    private final long id;
    private final long sessionId;
    private final long venueSeatId;
    private final String area;
    private final String rowNo;
    private final String seatNo;
    private final BigDecimal price;
    private final String status;
}
