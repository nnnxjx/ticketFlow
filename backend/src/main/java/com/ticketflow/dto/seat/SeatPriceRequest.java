package com.ticketflow.dto.seat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter
public class SeatPriceRequest {
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2)
    private BigDecimal price;
}
