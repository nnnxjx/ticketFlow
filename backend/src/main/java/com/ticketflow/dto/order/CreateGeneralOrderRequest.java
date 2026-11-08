package com.ticketflow.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateGeneralOrderRequest {
    @NotBlank @Size(max = 64) private String requestId;
    @NotNull @Positive private Long tierId;
    @NotNull @Positive private Integer quantity;
}
