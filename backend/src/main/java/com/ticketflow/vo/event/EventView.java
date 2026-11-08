package com.ticketflow.vo.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class EventView {
    private final long id;
    private final long organizerId;
    private final String title;
    private final String category;
    private final String description;
    private final String status;
}
