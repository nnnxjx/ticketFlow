package com.ticketflow.vo.venue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class VenueSeatView {
    private final long id;
    private final long venueId;
    private final String area;
    private final String rowNo;
    private final String seatNo;
}
