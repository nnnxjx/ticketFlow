package com.ticketflow.dto.session;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
public class SessionRequest {
    @NotNull private Long venueId;
    @NotNull @Pattern(regexp = "GENERAL|ASSIGNED") private String saleMode;
    @NotNull private LocalDateTime saleStart;
    @NotNull private LocalDateTime saleEnd;
    @NotNull private LocalDateTime startTime;
    @NotNull private LocalDateTime endTime;
    @NotNull private LocalDateTime checkinStart;
    @NotNull private LocalDateTime checkinEnd;
}
