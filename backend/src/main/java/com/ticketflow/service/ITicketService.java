package com.ticketflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ticketflow.entity.ticket.TicketEntity;
import com.ticketflow.vo.ticket.CheckinView;
import com.ticketflow.vo.ticket.TicketView;
import java.util.List;

public interface ITicketService extends IService<TicketEntity> {
    int issuePaid(int limit);
    List<TicketView> mine(long orderId);
    CheckinView checkin(String token);
}
