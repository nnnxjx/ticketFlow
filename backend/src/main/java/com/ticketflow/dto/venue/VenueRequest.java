package com.ticketflow.dto.venue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VenueRequest {
    @NotBlank @Size(max = 128) private String name;
    @Size(max = 255) private String address;
}
