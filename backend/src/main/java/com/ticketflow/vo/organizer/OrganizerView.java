package com.ticketflow.vo.organizer;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrganizerView {
    private final long id;
    private final String name;
    private final String status;

}
