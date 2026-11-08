package com.ticketflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ticketflow.entity.seat.SessionSeatEntity;
import com.ticketflow.vo.seat.SeatView;
import java.math.BigDecimal;
import java.util.List;

public interface ISeatCatalogService extends IService<SessionSeatEntity> {
    List<SeatView> initialize(long sessionId, BigDecimal defaultPrice);
    SeatView changePrice(long seatId, BigDecimal price);
    List<SeatView> mine(long sessionId);
    List<SeatView> publicList(long sessionId);
}
