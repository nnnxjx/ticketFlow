package com.ticketflow.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class EventRequest {
    @NotBlank @Size(max = 200) private String title;
    @Size(max = 64) private String category;
    private String description;
}
