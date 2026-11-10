package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.common.AppException;
import com.ticketflow.common.AsyncStockGate;
import com.ticketflow.common.CatalogAccess;
import com.ticketflow.common.Ids;
import com.ticketflow.common.SeatEventPublisher;
import com.ticketflow.common.UserContext;
import com.ticketflow.entity.event.EventEntity;
import com.ticketflow.entity.order.OrderEntity;
import com.ticketflow.entity.order.OrderItemEntity;
import com.ticketflow.entity.order.QuotaEntity;
import com.ticketflow.entity.refund.InventoryLogEntity;
import com.ticketflow.entity.refund.RefundEntity;
import com.ticketflow.entity.seat.SessionSeatEntity;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.ticket.TicketEntity;
import com.ticketflow.entity.tier.TierEntity;
import com.ticketflow.mapper.refund.RefundMapper;
import com.ticketflow.service.IEventService;
import com.ticketflow.service.IInventoryLogService;
import com.ticketflow.service.IOrderItemService;
import com.ticketflow.service.IOrderService;
import com.ticketflow.service.IQuotaService;
import com.ticketflow.service.IRefundService;
import com.ticketflow.service.ISeatCatalogService;
import com.ticketflow.service.ISessionService;
import com.ticketflow.service.ITicketService;
import com.ticketflow.service.ITierService;
import com.ticketflow.vo.refund.RefundView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RefundServiceImpl extends ServiceImpl<RefundMapper, RefundEntity> implements IRefundService {
    private static final Logger log = LoggerFactory.getLogger(RefundServiceImpl.class);
    @Autowired private IOrderService orders;
    @Autowired private IOrderItemService items;
    @Autowired private ITicketService tickets;
    @Autowired private ISessionService sessions;
    @Autowired private ITierService tiers;
    @Autowired private IEventService events;
    @Autowired private IQuotaService quotas;
    @Autowired private IInventoryLogService inventoryLogs;
    @Autowired private ISeatCatalogService seats;
    @Autowired private SeatEventPublisher seatEvents;
    @Autowired private AsyncStockGate stockGate;
    @Autowired private Ids ids;
    @Autowired private TransactionTemplate transactions;
    @Value("${ticketflow.refund.cutoff-hours:24}") private long cutoffHours;

    @Override
    public RefundView request(long orderId) {
        long userId = UserContext.getUserId();
        Boolean created = transactions.execute(status -> requestInTransaction(orderId, userId));
        if (created == null) throw new IllegalStateException("Refund transaction returned no result");
        if (!created) {
            RefundEntity existing = latestRefund(orderId);
            if (existing == null || (!"PROCESSING".equals(existing.getStatus())
                    && !"SUCCEEDED".equals(existing.getStatus()))) {
                throw CatalogAccess.conflict("订单状态已变化，请重新提交退款申请");
            }
        }
        return mine(orderId);
    }

    private boolean requestInTransaction(long orderId, long userId) {
        OrderEntity order = ownedOrder(orderId, userId);
        if ("REFUNDING".equals(order.getStatus()) || "REFUNDED".equals(order.getStatus())) return false;
        validateRefundable(order);
        List<TicketEntity> issuedTickets = orderTickets(order);
        if ("TICKETED".equals(order.getStatus()) && issuedTickets.size() != order.getQuantity()) {
            throw new IllegalStateException("Issued ticket count mismatch");
        }
        if ("PAID".equals(order.getStatus()) && !issuedTickets.isEmpty()) {
            throw new IllegalStateException("Paid order has partially issued tickets");
        }
        // 先抢订单状态；出票任务只能在 PAID 状态完成，避免退款和出票并行提交。

        //CAS乐观锁
        if (!orders.update(new LambdaUpdateWrapper<OrderEntity>()
                .eq(OrderEntity::getId, orderId).eq(OrderEntity::getStatus, order.getStatus())
                .set(OrderEntity::getStatus, "REFUNDING"))) return false;
        for (TicketEntity ticket : issuedTickets) {
            // 与检票的 UNUSED -> USED 条件更新竞争；任一票已用则整单回滚。
            if (!tickets.update(new LambdaUpdateWrapper<TicketEntity>()
                    .eq(TicketEntity::getId, ticket.getId()).eq(TicketEntity::getStatus, "UNUSED")
                    .set(TicketEntity::getStatus, "REFUNDING"))) {
                throw new AppException(HttpStatus.CONFLICT, "TICKET_USED", "订单中有已检票或不可退款的票券");
            }
        }
        RefundEntity refund = new RefundEntity();
        refund.setId(ids.next());
        refund.setOrderId(orderId);
        refund.setRefundNo("SIM-R-" + refund.getId());
        refund.setAmount(order.getAmount());
        refund.setPreviousOrderStatus(order.getStatus());
        refund.setStatus("PROCESSING");
        if (!save(refund)) throw new IllegalStateException("Refund insert failed");
        return true;
    }

    private void validateRefundable(OrderEntity order) {
        if (!"PAID".equals(order.getStatus()) && !"TICKETED".equals(order.getStatus())) {
            throw CatalogAccess.conflict("只有已支付或已出票订单可以退款");
        }
        if (!"GENERAL".equals(order.getSaleMode()) && !"ASSIGNED".equals(order.getSaleMode())) {
            throw CatalogAccess.conflict("订单销售模式不支持退款");
        }
        SessionEntity session = sessions.getById(order.getSessionId());
        if (session == null) throw CatalogAccess.notFound();
        if (!LocalDateTime.now(Clock.systemUTC()).isBefore(session.getStartTime().minusHours(cutoffHours))) {
            throw new AppException(HttpStatus.CONFLICT, "REFUND_CLOSED", "已超过退款截止时间");
        }
    }

    @Override
    public RefundView mine(long orderId) {
        ownedOrder(orderId, UserContext.getUserId());
        RefundEntity refund = latestRefund(orderId);
        if (refund == null) throw CatalogAccess.notFound();
        return view(refund);
    }

    @Override
    public int settlePending(int limit) {
        if (limit < 1 || limit > 500) throw CatalogAccess.invalid("扫描批量不合法");
        List<RefundEntity> pending = list(new LambdaQueryWrapper<RefundEntity>()
                .eq(RefundEntity::getStatus, "PROCESSING").orderByAsc(RefundEntity::getId).last("LIMIT " + limit));
        int completed = 0;
        for (RefundEntity refund : pending) {
            try {
                if (Boolean.TRUE.equals(transactions.execute(status -> settleOne(refund.getId())))) {
                    completed++;
                    creditSettledRefund(refund.getId());
                }
            } catch (RuntimeException ex) {
                log.error("Refund settlement failed for refund {}", refund.getId(), ex);
            }
        }
        return completed;
    }

    private boolean settleOne(long refundId) {
        RefundEntity refund = getById(refundId);
        if (refund == null || !"PROCESSING".equals(refund.getStatus())) return false;
        OrderEntity order = orders.getById(refund.getOrderId());
        if (order == null || !"REFUNDING".equals(order.getStatus())) throw new IllegalStateException("Refund order state mismatch");
        if (!update(new LambdaUpdateWrapper<RefundEntity>()
                .eq(RefundEntity::getId, refundId).eq(RefundEntity::getStatus, "PROCESSING")
                .set(RefundEntity::getStatus, "SUCCEEDED"))) return false;
        for (TicketEntity ticket : orderTickets(order)) {
            if (!tickets.update(new LambdaUpdateWrapper<TicketEntity>()
                    .eq(TicketEntity::getId, ticket.getId()).eq(TicketEntity::getStatus, "REFUNDING")
                    .set(TicketEntity::getStatus, "INVALID"))) {
                throw new IllegalStateException("Refund ticket transition failed");
            }
        }
        if (!orders.update(new LambdaUpdateWrapper<OrderEntity>()
                .eq(OrderEntity::getId, order.getId()).eq(OrderEntity::getStatus, "REFUNDING")
                .set(OrderEntity::getStatus, "REFUNDED"))) throw new IllegalStateException("Refund order transition failed");
        if ("GENERAL".equals(order.getSaleMode())) {
            releaseQuota(order);
            if (canResell(order)) restoreStock(refund, order);
        } else {
            restoreAssignedSeats(order);
        }
        return true;
    }

    private void restoreAssignedSeats(OrderEntity order) {
        List<SessionSeatEntity> sold = seats.list(new LambdaQueryWrapper<SessionSeatEntity>()
                .eq(SessionSeatEntity::getLockOrderId, order.getId()).eq(SessionSeatEntity::getStatus, "SOLD")
                .orderByAsc(SessionSeatEntity::getId));
        if (sold.size() != order.getQuantity()) throw new IllegalStateException("Sold seat count mismatch");
        String next = canResellAssigned(order) ? "FREE" : "UNAVAILABLE";
        for (SessionSeatEntity seat : sold) {
            // 只更改仍属于这笔订单的座位；全部更新随退款事务一起提交或回滚。
            if (!seats.update(new LambdaUpdateWrapper<SessionSeatEntity>()
                    .eq(SessionSeatEntity::getId, seat.getId()).eq(SessionSeatEntity::getStatus, "SOLD")
                    .eq(SessionSeatEntity::getLockOrderId, order.getId())
                    .set(SessionSeatEntity::getStatus, next)
                    .set(SessionSeatEntity::getLockOrderId, null)
                    .setSql("version = version + 1"))) {
                throw new IllegalStateException("Refund seat transition failed");
            }
            seatEvents.changed(order.getSessionId(), seat.getId(), seat.getVersion() + 1);
        }
    }

    private boolean canResellAssigned(OrderEntity order) {
        SessionEntity session = sessions.getById(order.getSessionId());
        EventEntity event = session == null ? null : events.getById(session.getEventId());
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        return session != null && event != null && "PUBLISHED".equals(event.getStatus())
                && "ON_SALE".equals(session.getStatus()) && now.isBefore(session.getStartTime())
                && !now.isBefore(session.getSaleStart()) && now.isBefore(session.getSaleEnd());
    }

    private void creditSettledRefund(long refundId) {
        RefundEntity refund = getById(refundId);
        if (refund == null || !"SUCCEEDED".equals(refund.getStatus())) return;
        OrderEntity order = orders.getById(refund.getOrderId());
        if (order == null) throw new IllegalStateException("Refund order missing: " + refund.getOrderId());
        if (order.getTierId() == null || !stockGate.isAsyncOrder(order.getUserId(), order.getRequestId())) return;
        // 有转售库存流水才恢复 Redis 库存；无论能否转售，都释放用户限购占用。
        boolean restock = inventoryLogs.getOne(new LambdaQueryWrapper<InventoryLogEntity>()
                .eq(InventoryLogEntity::getOperationId, "REFUND_RESALE:" + refundId)) != null;
        stockGate.credit(order.getTierId(), order.getUserId(), order.getQuantity(), "REFUND:" + refundId, restock);
    }

    private void releaseQuota(OrderEntity order) {
        if (!quotas.update(new LambdaUpdateWrapper<QuotaEntity>()
                .eq(QuotaEntity::getUserId, order.getUserId()).eq(QuotaEntity::getTierId, order.getTierId())
                .ge(QuotaEntity::getActiveQty, order.getQuantity())
                .setSql("active_qty = active_qty - {0}", order.getQuantity()))) {
            throw new IllegalStateException("Refund quota release failed");
        }
    }

    // 是否可转售
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

    private void restoreStock(RefundEntity refund, OrderEntity order) {
        // 记录库存流水
        InventoryLogEntity logEntry = new InventoryLogEntity();
        logEntry.setId(ids.next());
        logEntry.setOperationId("REFUND_RESALE:" + refund.getId());
        logEntry.setOrderId(order.getId());
        logEntry.setTierId(order.getTierId());
        logEntry.setOperation("REFUND_RESALE");
        logEntry.setQuantity(order.getQuantity());
        // 流水唯一键与库存更新同事务，重放不能导致重复加库存。
        if (!inventoryLogs.save(logEntry)) throw new IllegalStateException("Refund inventory log insert failed");
        if (!tiers.update(new LambdaUpdateWrapper<TierEntity>()
                .eq(TierEntity::getId, order.getTierId()).eq(TierEntity::getStatus, "ON_SALE")
                .apply("sale_start <= UTC_TIMESTAMP(3) AND sale_end > UTC_TIMESTAMP(3)")
                .apply("available_stock <= total_stock - {0}", order.getQuantity())
                .setSql("available_stock = available_stock + {0}", order.getQuantity()))) {
            throw new IllegalStateException("Refund stock restore failed");
        }
    }

    @Override
    public RefundView fail(long refundId, String failureCode) {
        transactions.execute(status -> failOne(refundId, failureCode));
        RefundEntity refund = getById(refundId);
        if (refund == null) throw CatalogAccess.notFound();
        return view(refund);
    }

    private boolean failOne(long refundId, String failureCode) {
        RefundEntity refund = getById(refundId);
        if (refund == null) throw CatalogAccess.notFound();
        if (!"PROCESSING".equals(refund.getStatus())) return false;
        OrderEntity order = orders.getById(refund.getOrderId());
        if (order == null || !"REFUNDING".equals(order.getStatus())) throw new IllegalStateException("Refund order state mismatch");
        if (!update(new LambdaUpdateWrapper<RefundEntity>()
                .eq(RefundEntity::getId, refundId).eq(RefundEntity::getStatus, "PROCESSING")
                .set(RefundEntity::getStatus, "FAILED").set(RefundEntity::getFailureCode, failureCode))) return false;
        for (TicketEntity ticket : orderTickets(order)) {
            if (!tickets.update(new LambdaUpdateWrapper<TicketEntity>()
                    .eq(TicketEntity::getId, ticket.getId()).eq(TicketEntity::getStatus, "REFUNDING")
                    .set(TicketEntity::getStatus, "UNUSED"))) throw new IllegalStateException("Ticket refund rollback failed");
        }
        if (!orders.update(new LambdaUpdateWrapper<OrderEntity>()
                .eq(OrderEntity::getId, order.getId()).eq(OrderEntity::getStatus, "REFUNDING")
                .set(OrderEntity::getStatus, refund.getPreviousOrderStatus()))) {
            throw new IllegalStateException("Order refund rollback failed");
        }
        return true;
    }

    private List<TicketEntity> orderTickets(OrderEntity order) {
        List<OrderItemEntity> orderItems = items.list(new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, order.getId()));
        if (orderItems.size() != order.getQuantity()) throw new IllegalStateException("Order item count mismatch");
        return orderItems.stream().map(item -> tickets.getOne(new LambdaQueryWrapper<TicketEntity>()
                .eq(TicketEntity::getOrderItemId, item.getId()))).filter(java.util.Objects::nonNull).toList();
    }

    private OrderEntity ownedOrder(long orderId, long userId) {
        OrderEntity order = orders.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) throw CatalogAccess.notFound();
        return order;
    }

    private RefundEntity latestRefund(long orderId) {
        return getOne(new LambdaQueryWrapper<RefundEntity>()
                .eq(RefundEntity::getOrderId, orderId).orderByDesc(RefundEntity::getId).last("LIMIT 1"));
    }

    private RefundView view(RefundEntity refund) {
        return new RefundView(refund.getId(), refund.getOrderId(), refund.getRefundNo(),
                refund.getAmount(), refund.getStatus(), refund.getFailureCode());
    }
}
