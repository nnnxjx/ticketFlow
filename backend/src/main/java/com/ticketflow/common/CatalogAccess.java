package com.ticketflow.common;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticketflow.entity.event.EventEntity;
import com.ticketflow.entity.organizer.OrganizerEntity;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.tier.TierEntity;
import com.ticketflow.entity.venue.VenueEntity;
import com.ticketflow.mapper.event.EventMapper;
import com.ticketflow.mapper.organizer.OrganizerMapper;
import com.ticketflow.mapper.session.SessionMapper;
import com.ticketflow.mapper.tier.TierMapper;
import com.ticketflow.mapper.venue.VenueMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class CatalogAccess {
    @Autowired private OrganizerMapper organizers;
    @Autowired private EventMapper events;
    @Autowired private SessionMapper sessions;
    @Autowired private TierMapper tiers;
    @Autowired private VenueMapper venues;

    public OrganizerEntity currentOrganizer() {
        // 首版每个负责人只关联一个主办方，后台接口从登录用户推导主办方身份。
        OrganizerEntity organizer = organizers.selectOne(new LambdaQueryWrapper<OrganizerEntity>()
                .eq(OrganizerEntity::getOwnerUserId, UserContext.getUserId()));
        if (organizer == null || !"ACTIVE".equals(organizer.getStatus())) throw notFound();
        return organizer;
    }

    public void organizerOwned(long organizerId) {
        if (!currentOrganizer().getId().equals(organizerId)) throw notFound();
    }

    public EventEntity eventOwned(long eventId) {
        EventEntity event = events.selectById(eventId);
        if (event == null) throw notFound();
        organizerOwned(event.getOrganizerId());
        return event;
    }

    public SessionEntity sessionOwned(long sessionId) {
        SessionEntity session = sessions.selectById(sessionId);
        if (session == null) throw notFound();
        eventOwned(session.getEventId());
        return session;
    }

    public TierEntity tierOwned(long tierId) {
        TierEntity tier = tiers.selectById(tierId);
        if (tier == null) throw notFound();
        sessionOwned(tier.getSessionId());
        return tier;
    }

    public VenueEntity venueOwned(long venueId) {
        VenueEntity venue = venues.selectById(venueId);
        if (venue == null) throw notFound();
        organizerOwned(venue.getOrganizerId());
        return venue;
    }

    public static AppException notFound() {
        return new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "资源不存在");
    }

    public static AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, "INVALID_STATE", message);
    }

    public static AppException invalid(String message) {
        return new AppException(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", message);
    }
}
