package com.ticketflow.vo.venue;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class VenueView {
    private final long id;
    private final long organizerId;
    private final String name;
    private final String address;
}
