package com.ticketflow.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CheckinRequest {
    @NotBlank @Size(max = 256) private String token;
}
