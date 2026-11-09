package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.common.CatalogAccess;
import com.ticketflow.common.Ids;
import com.ticketflow.common.AsyncStockGate;
import com.ticketflow.dto.session.SessionRequest;
import com.ticketflow.entity.event.EventEntity;
import com.ticketflow.entity.seat.SessionSeatEntity;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.tier.TierEntity;
import com.ticketflow.mapper.event.EventMapper;
import com.ticketflow.mapper.seat.SessionSeatMapper;
import com.ticketflow.mapper.session.SessionMapper;
import com.ticketflow.mapper.tier.TierMapper;
import com.ticketflow.service.ISessionService;
import com.ticketflow.vo.session.SessionView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class SessionServiceImpl extends ServiceImpl<SessionMapper, SessionEntity> implements ISessionService {
    @Autowired private CatalogAccess access;
    @Autowired private EventMapper events;
    @Autowired private TierMapper tiers;
    @Autowired private SessionSeatMapper seats;
    @Autowired private Ids ids;
    @Autowired private AsyncStockGate stockGate;

    @Override @Transactional
    public SessionView create(long eventId, SessionRequest request) {
        EventEntity event = access.eventOwned(eventId);
        if (!"DRAFT".equals(event.getStatus())) throw CatalogAccess.conflict("只有草稿演出可新增场次");
        SessionEntity session = new SessionEntity();
        session.setId(ids.next());
        session.setEventId(eventId);
        fill(session, request, event);
        session.setStatus("DRAFT");
        save(session);
        return view(session);
    }

    @Override @Transactional
    public SessionView update(long sessionId, SessionRequest request) {
        SessionEntity session = access.sessionOwned(sessionId);
        if (!"DRAFT".equals(session.getStatus())) throw CatalogAccess.conflict("只有草稿场次可编辑");
        EventEntity event = events.selectById(session.getEventId());
        // 已配置的票档窗口或座位归属依赖场次配置；修改前先清理这些子数据。
        if (tierCount(sessionId) > 0 || seatCount(sessionId) > 0) {
            throw CatalogAccess.conflict("请先清理票档或座位，再编辑场次");
        }
        fill(session, request, event);
        if (!update(session, new LambdaUpdateWrapper<SessionEntity>().eq(SessionEntity::getId, sessionId).eq(SessionEntity::getStatus, "DRAFT"))) {
            throw CatalogAccess.conflict("场次状态已变化");
        }
        return view(session);
    }

    @Override @Transactional
    public void delete(long sessionId) {
        access.sessionOwned(sessionId);
        // 与票档启用使用同一场次行锁，避免删除与 Redis 初始化交错。
        SessionEntity session = getOne(new LambdaQueryWrapper<SessionEntity>()
                .eq(SessionEntity::getId, sessionId).last("FOR UPDATE"));
        if (session == null) throw CatalogAccess.notFound();
        if (!"DRAFT".equals(session.getStatus())) {
            throw CatalogAccess.conflict("只能删除草稿场次");
        }
        if (baseMapper.countOrders(sessionId) > 0) throw CatalogAccess.conflict("已有订单的场次不能删除");
        // 锁定读取最新票档行；普通 SELECT 在 MySQL REPEATABLE READ 下可能仍看到旧快照。
        List<TierEntity> currentTiers = tiers.selectList(new LambdaQueryWrapper<TierEntity>()
                .eq(TierEntity::getSessionId, sessionId).orderByAsc(TierEntity::getId).last("FOR UPDATE"));
        // 场次及其票档或座位一并删除；事务失败时全部回滚，避免留下孤儿数据。
        tiers.delete(new LambdaQueryWrapper<TierEntity>().eq(TierEntity::getSessionId, sessionId));
        for (TierEntity tier : currentTiers) stockGate.removeDraft(tier.getId());
        seats.delete(new LambdaQueryWrapper<SessionSeatEntity>().eq(SessionSeatEntity::getSessionId, sessionId));
        if (!remove(new LambdaQueryWrapper<SessionEntity>().eq(SessionEntity::getId, sessionId).eq(SessionEntity::getStatus, "DRAFT"))) {
            throw CatalogAccess.conflict("场次状态已变化");
        }
    }

    @Override @Transactional
    public SessionView activate(long sessionId) {
        SessionEntity session = access.sessionOwned(sessionId);
        if ("GENERAL".equals(session.getSaleMode())) {
            if (tiers.selectCount(new LambdaQueryWrapper<TierEntity>().eq(TierEntity::getSessionId, sessionId).eq(TierEntity::getStatus, "ON_SALE")) == 0) {
                throw CatalogAccess.conflict("请先启用至少一个票档");
            }
        } else if (session.getVenueId() == null || seatCount(sessionId) == 0) {
            throw CatalogAccess.conflict("选座场次必须先初始化座位");
        }
        if (!update(new LambdaUpdateWrapper<SessionEntity>().eq(SessionEntity::getId, sessionId).eq(SessionEntity::getStatus, "DRAFT").set(SessionEntity::getStatus, "ON_SALE"))) {
            throw CatalogAccess.conflict("只有草稿场次可启用");
        }
        session.setStatus("ON_SALE");
        return view(session);
    }

    @Override @Transactional
    public SessionView stop(long sessionId) {
        SessionEntity session = access.sessionOwned(sessionId);
        if (!update(new LambdaUpdateWrapper<SessionEntity>().eq(SessionEntity::getId, sessionId).eq(SessionEntity::getStatus, "ON_SALE").set(SessionEntity::getStatus, "STOPPED"))) {
            throw CatalogAccess.conflict("只有可售场次可停售");
        }
        session.setStatus("STOPPED");
        return view(session);
    }

    @Override
    public List<SessionView> mine(long eventId) {
        access.eventOwned(eventId);
        return list(new LambdaQueryWrapper<SessionEntity>().eq(SessionEntity::getEventId, eventId).orderByAsc(SessionEntity::getStartTime))
                .stream().map(this::view).toList();
    }

    @Override
    public List<SessionView> publicList(long eventId) {
        EventEntity event = events.selectById(eventId);
        if (event == null || !"PUBLISHED".equals(event.getStatus())) throw CatalogAccess.notFound();
        return list(new LambdaQueryWrapper<SessionEntity>().eq(SessionEntity::getEventId, eventId).eq(SessionEntity::getStatus, "ON_SALE").orderByAsc(SessionEntity::getStartTime))
                .stream().map(this::view).toList();
    }

    private void fill(SessionEntity session, SessionRequest request, EventEntity event) {
        // 场次销售、演出及检票时间均使用 UTC，避免跨时区比较歧义。
        if (!request.getSaleStart().isBefore(request.getSaleEnd()) || request.getSaleEnd().isAfter(request.getStartTime())
                || !request.getStartTime().isBefore(request.getEndTime()) || !request.getCheckinStart().isBefore(request.getCheckinEnd())
                || request.getCheckinEnd().isAfter(request.getEndTime())) {
            throw CatalogAccess.invalid("场次时间范围不合法");
        }
        // 无座和选座场次都必须有场馆；购票用户需要明确的演出地点。
        if (request.getVenueId() == null) throw CatalogAccess.invalid("场次必须指定场馆");
        if (!access.venueOwned(request.getVenueId()).getOrganizerId().equals(event.getOrganizerId())) {
            throw CatalogAccess.invalid("场馆不属于演出主办方");
        }
        session.setVenueId(request.getVenueId());
        session.setSaleMode(request.getSaleMode());
        session.setSaleStart(request.getSaleStart());
        session.setSaleEnd(request.getSaleEnd());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setCheckinStart(request.getCheckinStart());
        session.setCheckinEnd(request.getCheckinEnd());
    }

    private long tierCount(long sessionId) { return tiers.selectCount(new LambdaQueryWrapper<TierEntity>().eq(TierEntity::getSessionId, sessionId)); }
    private long seatCount(long sessionId) { return seats.selectCount(new LambdaQueryWrapper<SessionSeatEntity>().eq(SessionSeatEntity::getSessionId, sessionId)); }
    private SessionView view(SessionEntity s) {
        return new SessionView(s.getId(), s.getEventId(), s.getVenueId(), s.getSaleMode(), s.getSaleStart(), s.getSaleEnd(), s.getStartTime(), s.getEndTime(), s.getCheckinStart(), s.getCheckinEnd(), s.getStatus());
    }
}
