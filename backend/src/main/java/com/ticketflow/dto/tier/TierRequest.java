package com.ticketflow.dto.tier;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
public class TierRequest {
    @NotBlank @Size(max = 80) private String name;
    @NotNull @DecimalMin("0.00") @Digits(integer = 10, fraction = 2) private BigDecimal price;
    @NotNull @Min(0) private Integer totalStock;
    @NotNull @Min(1) private Integer perUserLimit;
    @NotNull private LocalDateTime saleStart;
    @NotNull private LocalDateTime saleEnd;
}
