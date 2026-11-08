package com.ticketflow.dto.venue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class VenueSeatRequest {
    @NotBlank @Size(max = 64) private String area;
    @NotBlank @Size(max = 32) private String rowNo;
    @NotBlank @Size(max = 32) private String seatNo;
}
