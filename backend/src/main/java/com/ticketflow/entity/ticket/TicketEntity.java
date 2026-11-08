package com.ticketflow.entity.ticket;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter
@TableName("ticket")
public class TicketEntity {
    @TableId(type = IdType.INPUT) private Long id;
    private Long orderItemId;
    private byte[] tokenHash;
    private byte[] tokenCiphertext;
    private Short keyVersion;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
}
