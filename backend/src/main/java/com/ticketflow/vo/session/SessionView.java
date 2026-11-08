package com.ticketflow.vo.session;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class SessionView {
    private final long id;
    private final long eventId;
    private final Long venueId;
    private final String saleMode;
    private final LocalDateTime saleStart;
    private final LocalDateTime saleEnd;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final LocalDateTime checkinStart;
    private final LocalDateTime checkinEnd;
    private final String status;
}
