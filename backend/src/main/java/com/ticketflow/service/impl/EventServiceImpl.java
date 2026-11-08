package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.common.CatalogAccess;
import com.ticketflow.common.Ids;
import com.ticketflow.dto.event.EventRequest;
import com.ticketflow.entity.event.EventEntity;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.mapper.event.EventMapper;
import com.ticketflow.mapper.session.SessionMapper;
import com.ticketflow.service.IEventService;
import com.ticketflow.vo.event.EventView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class EventServiceImpl extends ServiceImpl<EventMapper, EventEntity> implements IEventService {
    @Autowired private CatalogAccess access;
    @Autowired private SessionMapper sessions;
    @Autowired private Ids ids;

    @Override @Transactional
    public EventView create(EventRequest request) {
        long organizerId = access.currentOrganizer().getId();
        EventEntity event = new EventEntity();
        event.setId(ids.next());
        event.setOrganizerId(organizerId);
        fill(event, request);
        event.setStatus("DRAFT");
        save(event);
        return view(event);
    }

    @Override @Transactional
    public EventView update(long eventId, EventRequest request) {
        EventEntity event = access.eventOwned(eventId);
        if (!"DRAFT".equals(event.getStatus())) throw CatalogAccess.conflict("只有草稿演出可编辑");
        fill(event, request);
        if (!update(event, new LambdaUpdateWrapper<EventEntity>().eq(EventEntity::getId, eventId).eq(EventEntity::getStatus, "DRAFT"))) {
            throw CatalogAccess.conflict("演出状态已变化");
        }
        return view(event);
    }

    @Override @Transactional
    public void delete(long eventId) {
        EventEntity event = access.eventOwned(eventId);
        if (!"DRAFT".equals(event.getStatus())) throw CatalogAccess.conflict("只能删除草稿演出");
        if (sessions.selectCount(new LambdaQueryWrapper<SessionEntity>().eq(SessionEntity::getEventId, eventId)) > 0) {
            throw CatalogAccess.conflict("请先删除该演出的场次");
        }
        remove(new LambdaQueryWrapper<EventEntity>().eq(EventEntity::getId, eventId).eq(EventEntity::getStatus, "DRAFT"));
    }

    @Override @Transactional
    public EventView publish(long eventId) {
        EventEntity event = access.eventOwned(eventId);
        if (!"DRAFT".equals(event.getStatus()) && !"OFF_SHELF".equals(event.getStatus())) throw CatalogAccess.conflict("演出不能发布");
        if (sessions.selectCount(new LambdaQueryWrapper<SessionEntity>().eq(SessionEntity::getEventId, eventId).eq(SessionEntity::getStatus, "ON_SALE")) == 0) {
            throw CatalogAccess.conflict("请先配置并启用至少一个场次");
        }
        // 状态条件更新防止并发发布与下架互相覆盖。
        if (!update(new LambdaUpdateWrapper<EventEntity>().eq(EventEntity::getId, eventId).eq(EventEntity::getStatus, event.getStatus()).set(EventEntity::getStatus, "PUBLISHED"))) {
            throw CatalogAccess.conflict("演出状态已变化");
        }
        event.setStatus("PUBLISHED");
        return view(event);
    }

    @Override @Transactional
    public EventView offShelf(long eventId) {
        EventEntity event = access.eventOwned(eventId);
        if (!update(new LambdaUpdateWrapper<EventEntity>().eq(EventEntity::getId, eventId).eq(EventEntity::getStatus, "PUBLISHED").set(EventEntity::getStatus, "OFF_SHELF"))) {
            throw CatalogAccess.conflict("只有已发布演出可下架");
        }
        event.setStatus("OFF_SHELF");
        return view(event);
    }

    @Override
    public List<EventView> mine() {
        long organizerId = access.currentOrganizer().getId();
        return list(new LambdaQueryWrapper<EventEntity>().eq(EventEntity::getOrganizerId, organizerId).orderByDesc(EventEntity::getId).last("LIMIT 100"))
                .stream().map(this::view).toList();
    }

    @Override
    public List<EventView> publicList() {
        return list(new LambdaQueryWrapper<EventEntity>().eq(EventEntity::getStatus, "PUBLISHED").orderByDesc(EventEntity::getId).last("LIMIT 100"))
                .stream().map(this::view).toList();
    }

    @Override
    public EventView publicDetail(long eventId) {
        EventEntity event = getById(eventId);
        if (event == null || !"PUBLISHED".equals(event.getStatus())) throw CatalogAccess.notFound();
        return view(event);
    }

    private void fill(EventEntity event, EventRequest request) {
        event.setTitle(request.getTitle().trim());
        event.setCategory(request.getCategory());
        event.setDescription(request.getDescription());
    }

    private EventView view(EventEntity event) {
        return new EventView(event.getId(), event.getOrganizerId(), event.getTitle(), event.getCategory(), event.getDescription(), event.getStatus());
    }
}
