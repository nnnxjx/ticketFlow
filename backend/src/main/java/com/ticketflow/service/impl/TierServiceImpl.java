package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.common.CatalogAccess;
import com.ticketflow.common.Ids;
import com.ticketflow.common.AsyncStockGate;
import com.ticketflow.dto.tier.TierRequest;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.event.EventEntity;
import com.ticketflow.entity.tier.TierEntity;
import com.ticketflow.mapper.event.EventMapper;
import com.ticketflow.mapper.session.SessionMapper;
import com.ticketflow.mapper.tier.TierMapper;
import com.ticketflow.service.ITierService;
import com.ticketflow.vo.tier.TierView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class TierServiceImpl extends ServiceImpl<TierMapper, TierEntity> implements ITierService {
    @Autowired private CatalogAccess access;
    @Autowired private EventMapper events;
    @Autowired private SessionMapper sessions;
    @Autowired private Ids ids;
    @Autowired private AsyncStockGate stockGate;

    @Override @Transactional
    public TierView create(long sessionId, TierRequest request) {
        SessionEntity session = editableSession(sessionId);
        TierEntity tier = new TierEntity();
        tier.setId(ids.next());
        tier.setSessionId(sessionId);
        fill(tier, request, session);
        tier.setStatus("DRAFT");
        try { save(tier); }
        catch (DuplicateKeyException ex) { throw CatalogAccess.conflict("该场次已有同名票档"); }
        return view(tier);
    }

    @Override @Transactional
    public TierView update(long tierId, TierRequest request) {
        access.tierOwned(tierId);
        TierEntity tier = new TierEntity();
        SessionEntity session = editableSession(tier.getSessionId());
        if (!"DRAFT".equals(tier.getStatus())) throw CatalogAccess.conflict("只有草稿票档可编辑");
        fill(tier, request, session);
        try {
            if (!update(tier, new LambdaUpdateWrapper<TierEntity>().eq(TierEntity::getId, tierId)
                    .eq(TierEntity::getStatus, "DRAFT"))) {
                throw CatalogAccess.conflict("票档状态已变化");
            }
        } catch (DuplicateKeyException ex) { throw CatalogAccess.conflict("该场次已有同名票档"); }
        return view(tier);
    }

    @Override @Transactional
    public void delete(long tierId) {
        TierEntity tier =access.tierOwned(tierId);
        editableSession(tier.getSessionId());
        if (!"DRAFT".equals(tier.getStatus())) throw CatalogAccess.conflict("只能删除草稿票档");
        if (!remove(new LambdaQueryWrapper<TierEntity>().eq(TierEntity::getId, tierId)
                .eq(TierEntity::getStatus, "DRAFT"))) {
            throw CatalogAccess.conflict("票档状态已变化");
        }
        stockGate.removeDraft(tierId);
    }

    @Override @Transactional
    public TierView activate(long tierId) {
        TierEntity tier = access.tierOwned(tierId);
        // 草稿期允许改票档；启用时锁定场次和票档，使用最终库存初始化 Redis。
        SessionEntity session = sessions.selectOne(new LambdaQueryWrapper<SessionEntity>()
                .eq(SessionEntity::getId, tier.getSessionId()).last("FOR UPDATE"));
        if (session == null || !"GENERAL".equals(session.getSaleMode()) || !"DRAFT".equals(session.getStatus())) {
            throw CatalogAccess.conflict("只能启用不选座草稿场次的票档");
        }
        tier = lockedTier(tierId);
        if (!"DRAFT".equals(tier.getStatus())) throw CatalogAccess.conflict("只有草稿票档可启用");
        stockGate.initialize(tier);
        if (!stockGate.ready(tierId)) throw CatalogAccess.conflict("异步库存不可用，请先对账");
        if (!update(new LambdaUpdateWrapper<TierEntity>().eq(TierEntity::getId, tierId)
                .eq(TierEntity::getStatus, "DRAFT")
                .set(TierEntity::getStatus, "ON_SALE"))) {
            throw CatalogAccess.conflict("只有草稿票档可启用");
        }
        tier.setStatus("ON_SALE");
        return view(tier);
    }

    @Override @Transactional
    public TierView stop(long tierId) {
        TierEntity tier = access.tierOwned(tierId);
        if (!update(new LambdaUpdateWrapper<TierEntity>().eq(TierEntity::getId, tierId).eq(TierEntity::getStatus, "ON_SALE").set(TierEntity::getStatus, "STOPPED"))) {
            throw CatalogAccess.conflict("只有可售票档可停售");
        }
        tier.setStatus("STOPPED");
        return view(tier);
    }

    private TierEntity lockedTier(long tierId) {
        TierEntity tier = getOne(new LambdaQueryWrapper<TierEntity>()
                .eq(TierEntity::getId, tierId).last("FOR UPDATE"));
        if (tier == null) throw CatalogAccess.notFound();
        return tier;
    }

    @Override
    public List<TierView> mine(long sessionId) {
        access.sessionOwned(sessionId);
        return list(new LambdaQueryWrapper<TierEntity>().eq(TierEntity::getSessionId, sessionId).orderByAsc(TierEntity::getPrice))
                .stream().map(this::view).toList();
    }

    @Override
    public List<TierView> publicList(long sessionId) {
        SessionEntity session = sessions.selectById(sessionId);
        if (session == null || !"GENERAL".equals(session.getSaleMode()) || !"ON_SALE".equals(session.getStatus())) throw CatalogAccess.notFound();
        // 公开查询还必须检查演出已发布，不能仅凭票档状态展示草稿活动。
        EventEntity event = events.selectById(session.getEventId());
        if (event == null || !"PUBLISHED".equals(event.getStatus())) throw CatalogAccess.notFound();
        return list(new LambdaQueryWrapper<TierEntity>().eq(TierEntity::getSessionId, sessionId).eq(TierEntity::getStatus, "ON_SALE").orderByAsc(TierEntity::getPrice))
                .stream().map(this::view).toList();
    }

    private SessionEntity editableSession(long sessionId) {
        SessionEntity session = access.sessionOwned(sessionId);
        if (!"GENERAL".equals(session.getSaleMode()) || !"DRAFT".equals(session.getStatus())) {
            throw CatalogAccess.conflict("只能在不选座的草稿场次管理票档");
        }
        return session;
    }

    private void fill(TierEntity tier, TierRequest request, SessionEntity session) {
        if (!request.getSaleStart().isBefore(request.getSaleEnd()) || request.getSaleStart().isBefore(session.getSaleStart())
                || request.getSaleEnd().isAfter(session.getSaleEnd())) throw CatalogAccess.invalid("票档销售时间必须在场次销售窗口内");
        tier.setName(request.getName().trim());
        tier.setPrice(request.getPrice());
        tier.setTotalStock(request.getTotalStock());
        tier.setAvailableStock(request.getTotalStock()); // 草稿阶段尚未产生订单。
        tier.setPerUserLimit(request.getPerUserLimit());
        tier.setSaleStart(request.getSaleStart());
        tier.setSaleEnd(request.getSaleEnd());
    }

    private TierView view(TierEntity t) {
        return new TierView(t.getId(), t.getSessionId(), t.getName(), t.getPrice(), t.getTotalStock(), t.getAvailableStock(), t.getPerUserLimit(), t.getSaleStart(), t.getSaleEnd(), t.getStatus());
    }
}
