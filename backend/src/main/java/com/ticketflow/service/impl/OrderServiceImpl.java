package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.common.AppException;
import com.ticketflow.common.CatalogAccess;
import com.ticketflow.common.Ids;
import com.ticketflow.common.UserContext;
import com.ticketflow.dto.order.CreateGeneralOrderRequest;
import com.ticketflow.entity.event.EventEntity;
import com.ticketflow.entity.order.OrderEntity;
import com.ticketflow.entity.order.OrderItemEntity;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.tier.TierEntity;
import com.ticketflow.mapper.order.OrderMapper;
import com.ticketflow.service.IEventService;
import com.ticketflow.service.IOrderItemService;
import com.ticketflow.service.IOrderService;
import com.ticketflow.service.IQuotaService;
import com.ticketflow.service.ISessionService;
import com.ticketflow.service.ITierService;
import com.ticketflow.vo.order.OrderItemView;
import com.ticketflow.vo.order.OrderPageView;
import com.ticketflow.vo.order.OrderView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements IOrderService {
    @Autowired private ITierService tiers;
    @Autowired private ISessionService sessions;
    @Autowired private IEventService events;
    @Autowired private IQuotaService quotas;
    @Autowired private IOrderItemService items;
    @Autowired private Ids ids;
    @Autowired private TransactionTemplate transactions;
    @Value("${ticketflow.order.payment-timeout-minutes:15}") private long timeoutMinutes;

    @Override
    public OrderView createGeneral(CreateGeneralOrderRequest request) {
        long userId = UserContext.getUserId();
        String requestId = request.getRequestId().trim();
        // 同一次购票重复提交时返回原订单，避免再次扣库存。
        OrderEntity existing = findByRequest(userId, requestId);
        if (existing != null) return repeated(existing, request);
        try {
            Long orderId = transactions.execute(status -> writeOrder(userId, requestId, request));
            if (orderId == null) throw new IllegalStateException("Order transaction returned no ID");
            return detailForUser(orderId, userId);
        } catch (DuplicateKeyException ex) {
            // 两个 HTTP 请求同时使用相同 requestId 时，由唯一键识别已提交的订单。
            existing = findByRequest(userId, requestId);
            if (existing != null) return repeated(existing, request);
            throw ex;
        }
    }


    private long writeOrder(long userId, String requestId, CreateGeneralOrderRequest request) {
        int quantity = request.getQuantity();
        if (quantity < 1) throw CatalogAccess.invalid("单次购票数量不合法");
        TierEntity tier = tiers.getById(request.getTierId());
        if (tier == null) throw CatalogAccess.notFound();
        SessionEntity session = sessions.getById(tier.getSessionId());
        EventEntity event = session == null ? null : events.getById(session.getEventId());
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        if (event == null || !"PUBLISHED".equals(event.getStatus()) || !"GENERAL".equals(session.getSaleMode())
                || !"ON_SALE".equals(session.getStatus()) || !"ON_SALE".equals(tier.getStatus())
                || now.isBefore(session.getSaleStart()) || !now.isBefore(session.getSaleEnd())
                || now.isBefore(tier.getSaleStart()) || !now.isBefore(tier.getSaleEnd())) {
            throw CatalogAccess.conflict("场次或票档当前不可购买");
        }

        OrderEntity order = new OrderEntity();
        order.setId(ids.next());
        order.setUserId(userId);
        order.setSessionId(session.getId());
        order.setTierId(tier.getId());
        order.setRequestId(requestId);
        order.setSaleMode("GENERAL");
        order.setQuantity(quantity);
        order.setAmount(tier.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setStatus("PENDING_PAYMENT");
        order.setExpireAt(now.plusMinutes(timeoutMinutes));
        // 先写订单占住唯一请求键；后续任一操作失败，事务会连订单一起回滚。
        if (!save(order)) throw new IllegalStateException("Order insert failed");
        quotas.reserve(userId, tier.getId(), quantity, tier.getPerUserLimit());
        // 条件更新仍由数据库判断剩余库存；并发下不能先读库存再普通 updateById。
        if (!tiers.update(new LambdaUpdateWrapper<TierEntity>()
                .eq(TierEntity::getId, tier.getId()).eq(TierEntity::getStatus, "ON_SALE")
                .le(TierEntity::getSaleStart, now).gt(TierEntity::getSaleEnd, now)
                .ge(TierEntity::getAvailableStock, quantity)
                .setSql("available_stock = available_stock - {0}", quantity))) {
            throw new AppException(HttpStatus.CONFLICT, "SOLD_OUT", "票档库存不足或已停售");
        }
        for (int i = 0; i < quantity; i++) {
            OrderItemEntity item = new OrderItemEntity();
            item.setId(ids.next());
            item.setOrderId(order.getId());
            item.setTierId(tier.getId());
            item.setItemNameSnapshot(tier.getName());
            item.setPriceSnapshot(tier.getPrice());
            if (!items.save(item)) throw new IllegalStateException("Order item insert failed");
        }
        return order.getId();
    }

    @Override
    public OrderView detail(long orderId) { return detailForUser(orderId, UserContext.getUserId()); }

    private OrderView detailForUser(long orderId, long userId) {
        OrderEntity order = getOne(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getId, orderId).eq(OrderEntity::getUserId, userId));
        if (order == null) throw CatalogAccess.notFound();
        List<OrderItemView> itemViews = items.list(new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, orderId).orderByAsc(OrderItemEntity::getId))
                .stream().map(i -> new OrderItemView(i.getId(), i.getItemNameSnapshot(), i.getPriceSnapshot())).toList();
        return view(order, itemViews);
    }

    @Override
    public OrderPageView mine(int page, int size) {
        if (page < 1 || size < 1 || size > 100) throw CatalogAccess.invalid("页码或每页数量不合法");
        long offset = ((long) page - 1) * size;
        if (offset > Integer.MAX_VALUE) throw CatalogAccess.invalid("页码过大");
        long userId = UserContext.getUserId();
        long total = count(new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getUserId, userId));
        // LIMIT/OFFSET 仅由已验证的整数生成，避免把用户输入拼进 SQL。
        List<OrderView> orders = list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId).orderByDesc(OrderEntity::getCreatedAt, OrderEntity::getId)
                .last("LIMIT " + size + " OFFSET " + offset)).stream()
                .map(order -> view(order, List.of())).toList();
        return new OrderPageView(page, size, total, orders);
    }

    private OrderEntity findByRequest(long userId, String requestId) {
        return getOne(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId).eq(OrderEntity::getRequestId, requestId));
    }

    private OrderView repeated(OrderEntity order, CreateGeneralOrderRequest request) {
        if (!"GENERAL".equals(order.getSaleMode()) || !order.getTierId().equals(request.getTierId())
                || !order.getQuantity().equals(request.getQuantity())) {
            throw new AppException(HttpStatus.CONFLICT, "REQUEST_ID_CONFLICT", "相同 requestId 的购票参数不同");
        }
        return detailForUser(order.getId(), order.getUserId());
    }

    private OrderView view(OrderEntity order, List<OrderItemView> itemViews) {
        return new OrderView(order.getId(), order.getSessionId(), order.getTierId(), order.getRequestId(),
                order.getSaleMode(), order.getQuantity(), order.getAmount(), order.getStatus(),
                order.getExpireAt(), order.getCreatedAt(), itemViews);
    }
}
