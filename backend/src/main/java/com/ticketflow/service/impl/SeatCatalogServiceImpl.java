package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.common.CatalogAccess;
import com.ticketflow.common.Ids;
import com.ticketflow.entity.event.EventEntity;
import com.ticketflow.entity.seat.SessionSeatEntity;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.venue.VenueSeatEntity;
import com.ticketflow.mapper.event.EventMapper;
import com.ticketflow.mapper.seat.SessionSeatMapper;
import com.ticketflow.mapper.session.SessionMapper;
import com.ticketflow.mapper.venue.VenueSeatMapper;
import com.ticketflow.service.ISeatCatalogService;
import com.ticketflow.vo.seat.SeatView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SeatCatalogServiceImpl extends ServiceImpl<SessionSeatMapper, SessionSeatEntity> implements ISeatCatalogService {
    @Autowired private CatalogAccess access;
    @Autowired private SessionMapper sessions;
    @Autowired private EventMapper events;
    @Autowired private VenueSeatMapper venueSeats;
    @Autowired private Ids ids;

    @Override @Transactional
    public List<SeatView> initialize(long sessionId, BigDecimal defaultPrice) {
        SessionEntity session = access.sessionOwned(sessionId);
        if (!"ASSIGNED".equals(session.getSaleMode()) || !"DRAFT".equals(session.getStatus()) || session.getVenueId() == null) {
            throw CatalogAccess.conflict("只能初始化选座草稿场次");
        }
        validatePrice(defaultPrice);
        if (count(new LambdaQueryWrapper<SessionSeatEntity>().eq(SessionSeatEntity::getSessionId, sessionId)) > 0) {
            throw CatalogAccess.conflict("场次座位已经初始化");
        }
        List<VenueSeatEntity> templates = venueSeats.selectList(new LambdaQueryWrapper<VenueSeatEntity>()
                .eq(VenueSeatEntity::getVenueId, session.getVenueId()).orderByAsc(VenueSeatEntity::getId));
        if (templates.isEmpty()) throw CatalogAccess.conflict("场馆没有座位模板");
        // 每个场次复制自己的座位状态，物理座位模板不会被订单直接修改。
        try {
            for (VenueSeatEntity template : templates) {
                SessionSeatEntity seat = new SessionSeatEntity();
                seat.setId(ids.next());
                seat.setSessionId(sessionId);
                seat.setVenueSeatId(template.getId());
                seat.setPrice(defaultPrice);
                seat.setStatus("FREE");
                seat.setVersion(0L);
                save(seat);
            }
        } catch (DuplicateKeyException ex) { throw CatalogAccess.conflict("场次座位已经初始化"); }
        return mine(sessionId);
    }

    @Override @Transactional
    public SeatView changePrice(long seatId, BigDecimal price) {
        SessionSeatEntity seat = getById(seatId);
        if (seat == null) throw CatalogAccess.notFound();
        SessionEntity session = access.sessionOwned(seat.getSessionId());
        if (!"DRAFT".equals(session.getStatus()) || !"FREE".equals(seat.getStatus())) {
            throw CatalogAccess.conflict("只有草稿场次的空闲座位可改价");
        }
        validatePrice(price);
        if (!update(new LambdaUpdateWrapper<SessionSeatEntity>().eq(SessionSeatEntity::getId, seatId)
                .eq(SessionSeatEntity::getStatus, "FREE").eq(SessionSeatEntity::getVersion, seat.getVersion())
                .set(SessionSeatEntity::getPrice, price).set(SessionSeatEntity::getVersion, seat.getVersion() + 1))) {
            throw CatalogAccess.conflict("座位状态或价格已变化");
        }
        seat.setPrice(price);
        return view(seat, venueSeats.selectById(seat.getVenueSeatId()));
    }

    @Override
    public List<SeatView> mine(long sessionId) {
        access.sessionOwned(sessionId);
        return seats(sessionId);
    }

    @Override
    public List<SeatView> publicList(long sessionId) {
        SessionEntity session = sessions.selectById(sessionId);
        if (session == null || !"ASSIGNED".equals(session.getSaleMode()) || !"ON_SALE".equals(session.getStatus())) throw CatalogAccess.notFound();
        EventEntity event = events.selectById(session.getEventId());
        if (event == null || !"PUBLISHED".equals(event.getStatus())) throw CatalogAccess.notFound();
        return seats(sessionId);
    }

    private List<SeatView> seats(long sessionId) {
        List<SessionSeatEntity> seats = list(new LambdaQueryWrapper<SessionSeatEntity>().eq(SessionSeatEntity::getSessionId, sessionId)
                .orderByAsc(SessionSeatEntity::getVenueSeatId));
        if (seats.isEmpty()) return List.of();
        Map<Long, VenueSeatEntity> templates = venueSeats.selectBatchIds(seats.stream().map(SessionSeatEntity::getVenueSeatId).toList())
                .stream().collect(Collectors.toMap(VenueSeatEntity::getId, Function.identity()));
        return seats.stream().map(seat -> view(seat, templates.get(seat.getVenueSeatId()))).toList();
    }

    private SeatView view(SessionSeatEntity s, VenueSeatEntity template) {
        if (template == null) throw CatalogAccess.conflict("座位模板缺失");
        return new SeatView(s.getId(), s.getSessionId(), s.getVenueSeatId(), template.getArea(), template.getRowNo(), template.getSeatNo(), s.getPrice(), s.getStatus());
    }

    private void validatePrice(BigDecimal price) {
        if (price == null || price.signum() < 0 || price.scale() > 2) throw CatalogAccess.invalid("座位价格不合法");
    }
}
