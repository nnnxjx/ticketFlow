package com.ticketflow.dto.session;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@Schema(description = "场次配置：saleStart < saleEnd <= startTime < endTime；checkinStart < startTime 且 checkinStart < checkinEnd <= endTime；售票与检票允许重叠")
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
