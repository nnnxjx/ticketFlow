package com.ticketflow.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 仅提交场次和座位 ID；价格与座位位置由服务端读取。 */
@Getter @Setter
public class CreateAssignedOrderRequest {
    @NotBlank @Size(max = 64) private String requestId;
    @NotNull @Positive private Long sessionId;
    @NotEmpty private List<@NotNull @Positive Long> seatIds;
}
