package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.common.AppException;
import com.ticketflow.common.AsyncStockGate;
import com.ticketflow.common.CatalogAccess;
import com.ticketflow.common.Ids;
import com.ticketflow.common.UserContext;
import com.ticketflow.dto.order.CreateGeneralOrderRequest;
import com.ticketflow.entity.event.EventEntity;
import com.ticketflow.entity.order.OrderEntity;
import com.ticketflow.entity.order.OrderItemEntity;
import com.ticketflow.entity.order.QuotaEntity;
import com.ticketflow.entity.payment.PaymentEntity;
import com.ticketflow.entity.refund.InventoryLogEntity;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.tier.TierEntity;
import com.ticketflow.mapper.order.OrderMapper;
import com.ticketflow.service.IEventService;
import com.ticketflow.service.IInventoryLogService;
import com.ticketflow.service.IOrderItemService;
import com.ticketflow.service.IOrderService;
import com.ticketflow.service.IQuotaService;
import com.ticketflow.service.ISessionService;
import com.ticketflow.service.ITierService;
import com.ticketflow.service.IPaymentService;
import com.ticketflow.vo.order.OrderItemView;
import com.ticketflow.vo.order.OrderPageView;
import com.ticketflow.vo.order.OrderView;
import com.ticketflow.vo.payment.PaymentView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, OrderEntity> implements IOrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    @Autowired private ITierService tiers;
    @Autowired private ISessionService sessions;
    @Autowired private IEventService events;
    @Autowired private IQuotaService quotas;
    @Autowired private IOrderItemService items;
    @Autowired private IPaymentService payments;
    @Autowired private IInventoryLogService inventoryLogs;
    @Autowired private AsyncStockGate stockGate;
    @Autowired private Ids ids;
    @Autowired private TransactionTemplate transactions;
    @Value("${ticketflow.order.payment-timeout-minutes:15}") private long timeoutMinutes;

    // 旧同步下单实现保留作参考，不再注册到业务接口。
    // public OrderView createGeneral(CreateGeneralOrderRequest request) {
    //     long userId = UserContext.getUserId();
    //     String requestId = request.getRequestId().trim();
    //     OrderEntity existing = findByRequest(userId, requestId);
    //     if (existing != null) return repeated(existing, request);
    //     try {
    //         Long orderId = transactions.execute(status -> writeOrder(userId, requestId, request));
    //         if (orderId == null) throw new IllegalStateException("Order transaction returned no ID");
    //         return detailForUser(orderId, userId);
    //     } catch (DuplicateKeyException ex) {
    //         existing = findByRequest(userId, requestId);
    //         if (existing != null) return repeated(existing, request);
    //         throw ex;
    //     }
    // }


    @Override
    public long createAsync(long userId, String requestId, long tierId, int quantity) {
        // 消费者显式传入用户身份；后台线程不能依赖 HTTP 请求的 UserContext。
        CreateGeneralOrderRequest request = new CreateGeneralOrderRequest();
        request.setRequestId(requestId);
        request.setTierId(tierId);
        request.setQuantity(quantity);
        OrderEntity existing = findByRequest(userId, requestId);
        if (existing != null) {
            validateSameRequest(existing, request);
            return existing.getId();
        }
        // 订单唯一键兜住重复 MQ 消息；事务内再做最终库存和限购判断。
        try {
            Long orderId = transactions.execute(status -> writeOrder(userId, requestId, request));
            if (orderId == null) throw new IllegalStateException("Order transaction returned no ID");
            return orderId;
        } catch (DuplicateKeyException ex) {
            existing = findByRequest(userId, requestId);
            if (existing == null) throw ex;
            validateSameRequest(existing, request);
            return existing.getId();
        }
    }

    private long writeOrder(long userId, String requestId, CreateGeneralOrderRequest request) {
        // Redis 仅做高并发入口预占，MySQL 票档库存和限购占用是最终交易依据。
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

    // private OrderView repeated(OrderEntity order, CreateGeneralOrderRequest request) {
    //     validateSameRequest(order, request);
    //     return detailForUser(order.getId(), order.getUserId());
    // }

    private void validateSameRequest(OrderEntity order, CreateGeneralOrderRequest request) {
        if (!"GENERAL".equals(order.getSaleMode()) || !order.getTierId().equals(request.getTierId())
                || !order.getQuantity().equals(request.getQuantity())) {
            throw new AppException(HttpStatus.CONFLICT, "REQUEST_ID_CONFLICT", "相同 requestId 的购票参数不同");
        }
    }

    private OrderView view(OrderEntity order, List<OrderItemView> itemViews) {
        return new OrderView(order.getId(), order.getSessionId(), order.getTierId(), order.getRequestId(),
                order.getSaleMode(), order.getQuantity(), order.getAmount(), order.getStatus(),
                order.getExpireAt(), order.getCreatedAt(), itemViews);
    }

    @Override
    public OrderView cancel(long orderId) {
        long userId = UserContext.getUserId();
        transactions.execute(status -> cancelPending(orderId, userId, false));
        // 提交 MySQL 后再回补 Redis；重复取消时 Lua 标记允许安全重试。
        creditCancelled(orderId);
        return detailForUser(orderId, userId);
    }

    @Override
    public PaymentView pay(long orderId) {
        long userId = UserContext.getUserId();
        transactions.execute(status -> payPending(orderId, userId));
        OrderEntity order = getOwnedOrder(orderId, userId);
        if (!"PAID".equals(order.getStatus()) && !"TICKETED".equals(order.getStatus())) {
            throw paymentConflict(order);
        }
        PaymentEntity payment = payments.getOne(new LambdaQueryWrapper<PaymentEntity>()
                .eq(PaymentEntity::getOrderId, orderId));
        if (payment == null) throw new IllegalStateException("Paid order has no payment record");
        return new PaymentView(orderId, payment.getPaymentNo(), payment.getAmount(), payment.getStatus(), payment.getPaidAt());
    }

    @Override
    public int closeExpired(int limit) {
        if (limit < 1 || limit > 500) throw CatalogAccess.invalid("扫描批量不合法");
        List<OrderEntity> expired = list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getStatus, "PENDING_PAYMENT")
                .apply("expire_at <= UTC_TIMESTAMP(3)")
                .orderByAsc(OrderEntity::getExpireAt).last("LIMIT " + limit));
        int closed = 0;
        for (OrderEntity order : expired) {
            try {
                Boolean changed = transactions.execute(status -> cancelPending(order.getId(), order.getUserId(), true));
                if (Boolean.TRUE.equals(changed)) closed++;
                creditCancelled(order.getId());
            } catch (RuntimeException ex) {
                // 单笔异常不能阻止本轮其他到期订单关单；失败订单下一轮仍可重扫。
                log.error("Failed to close expired order {}", order.getId(), ex);
            }
        }
        return closed;
    }

    private boolean cancelPending(long orderId, long userId, boolean expiredOnly) {
        OrderEntity order = getOwnedOrder(orderId, userId);
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            if (expiredOnly || "CANCELLED".equals(order.getStatus())) return false;
            throw CatalogAccess.conflict("只有待支付订单可以取消");
        }
        LambdaUpdateWrapper<OrderEntity> transition = new LambdaUpdateWrapper<OrderEntity>()
                .eq(OrderEntity::getId, orderId).eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getStatus, "PENDING_PAYMENT")
                .set(OrderEntity::getStatus, "CANCELLED");
        if (expiredOnly) transition.apply("expire_at <= UTC_TIMESTAMP(3)");
        // 状态转换成功者才释放库存；与支付并发时只有一方会更新到一行。
        if (!update(transition)) {
            if (expiredOnly || "CANCELLED".equals(getOwnedOrder(orderId, userId).getStatus())) return false;
            throw CatalogAccess.conflict("订单状态已变化");
        }
        releaseGeneralReservation(order);
        return true;
    }

    private void releaseGeneralReservation(OrderEntity order) {
        if (!"GENERAL".equals(order.getSaleMode()) || order.getTierId() == null) {
            throw new IllegalStateException("Unsupported order sale mode");
        }
        int quantity = order.getQuantity();
        // 与建单时保持“限购行 → 票档行”的更新顺序，降低交叉锁导致的死锁概率。
        if (!quotas.update(new LambdaUpdateWrapper<QuotaEntity>()
                .eq(QuotaEntity::getUserId, order.getUserId()).eq(QuotaEntity::getTierId, order.getTierId())
                .ge(QuotaEntity::getActiveQty, quantity)
                .setSql("active_qty = active_qty - {0}", quantity))) {
            throw new IllegalStateException("Quota release failed");
        }
        // 取消后始终释放用户限购；只有还可销售的票才重新进入可售库存。
        if (!canResell(order)) return;
        InventoryLogEntity logEntry = new InventoryLogEntity();
        logEntry.setId(ids.next());
        logEntry.setOperationId("ORDER_RELEASE:" + order.getId());
        logEntry.setOrderId(order.getId());
        logEntry.setTierId(order.getTierId());
        logEntry.setOperation("ORDER_RELEASE");
        logEntry.setQuantity(quantity);
        if (!inventoryLogs.save(logEntry)) throw new IllegalStateException("Cancellation inventory log insert failed");
        if (!tiers.update(new LambdaUpdateWrapper<TierEntity>()
                .eq(TierEntity::getId, order.getTierId()).eq(TierEntity::getStatus, "ON_SALE")
                .apply("sale_start <= UTC_TIMESTAMP(3) AND sale_end > UTC_TIMESTAMP(3)")
                .apply("available_stock <= total_stock - {0}", quantity)
                .setSql("available_stock = available_stock + {0}", quantity))) {
            throw new IllegalStateException("Stock release failed");
        }
    }

    private boolean canResell(OrderEntity order) {
        SessionEntity session = sessions.getById(order.getSessionId());
        TierEntity tier = tiers.getById(order.getTierId());
        EventEntity event = session == null ? null : events.getById(session.getEventId());
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        return session != null && tier != null && event != null
                && "PUBLISHED".equals(event.getStatus()) && "ON_SALE".equals(session.getStatus())
                && "ON_SALE".equals(tier.getStatus()) && now.isBefore(session.getStartTime())
                && !now.isBefore(session.getSaleStart()) && now.isBefore(session.getSaleEnd())
                && !now.isBefore(tier.getSaleStart()) && now.isBefore(tier.getSaleEnd());
    }

    private void creditCancelled(long orderId) {
        OrderEntity order = getById(orderId);
        if (order != null && "CANCELLED".equals(order.getStatus()) && order.getTierId() != null
                && stockGate.isAsyncOrder(order.getUserId(), order.getRequestId())) {
            // 以已提交的库存流水决定 Redis 是否增加库存，避免跨越销售截止点时判断不一致。
            boolean restock = inventoryLogs.getOne(new LambdaQueryWrapper<InventoryLogEntity>()
                    .eq(InventoryLogEntity::getOperationId, "ORDER_RELEASE:" + orderId)) != null;
            stockGate.credit(order.getTierId(), order.getUserId(), order.getQuantity(), "CANCEL:" + orderId, restock);
        }
    }

    private boolean payPending(long orderId, long userId) {
        OrderEntity order = getOwnedOrder(orderId, userId);
        if ("PAID".equals(order.getStatus()) || "TICKETED".equals(order.getStatus())) return false;
        if (!"PENDING_PAYMENT".equals(order.getStatus())) throw paymentConflict(order);
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        if (!update(new LambdaUpdateWrapper<OrderEntity>()
                .eq(OrderEntity::getId, orderId).eq(OrderEntity::getUserId, userId)
                .eq(OrderEntity::getStatus, "PENDING_PAYMENT")
                .apply("expire_at > UTC_TIMESTAMP(3)")
                .set(OrderEntity::getStatus, "PAID").set(OrderEntity::getPaidAt, now))) {
            return false;
        }
        PaymentEntity payment = new PaymentEntity();
        payment.setId(ids.next());
        payment.setOrderId(orderId);
        payment.setPaymentNo("SIM-" + orderId);
        payment.setAmount(order.getAmount());
        payment.setStatus("SUCCEEDED");
        payment.setPaidAt(now);
        if (!payments.save(payment)) throw new IllegalStateException("Payment record insert failed");
        return true;
    }

    private OrderEntity getOwnedOrder(long orderId, long userId) {
        OrderEntity order = getOne(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getId, orderId).eq(OrderEntity::getUserId, userId));
        if (order == null) throw CatalogAccess.notFound();
        return order;
    }

    private AppException paymentConflict(OrderEntity order) {
        if ("CANCELLED".equals(order.getStatus())) {
            return new AppException(HttpStatus.CONFLICT, "ORDER_CANCELLED", "订单已取消");
        }
        if ("PENDING_PAYMENT".equals(order.getStatus())) {
            return new AppException(HttpStatus.CONFLICT, "ORDER_EXPIRED", "订单已超过支付时限");
        }
        return new AppException(HttpStatus.CONFLICT, "ORDER_NOT_PAYABLE", "订单不可支付");
    }
}
