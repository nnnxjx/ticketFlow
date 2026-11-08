package com.ticketflow.entity.order;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("user_tier_quota")
public class QuotaEntity {
    private Long userId;
    private Long tierId;
    private Integer activeQty;
}
