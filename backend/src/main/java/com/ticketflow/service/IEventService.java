package com.ticketflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ticketflow.dto.event.EventRequest;
import com.ticketflow.entity.event.EventEntity;
import com.ticketflow.vo.event.EventView;
import java.util.List;

public interface IEventService extends IService<EventEntity> {
    EventView create(EventRequest request);
    EventView update(long eventId, EventRequest request);
    void delete(long eventId);
    EventView publish(long eventId);
    EventView offShelf(long eventId);
    List<EventView> mine();
    List<EventView> publicList();
    EventView publicDetail(long eventId);
}
