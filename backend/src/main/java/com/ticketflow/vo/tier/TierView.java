package com.ticketflow.vo.tier;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @AllArgsConstructor
public class TierView {
    private final long id;
    private final long sessionId;
    private final String name;
    private final BigDecimal price;
    private final int totalStock;
    private final int availableStock;
    private final int perUserLimit;
    private final LocalDateTime saleStart;
    private final LocalDateTime saleEnd;
    private final String status;
}
