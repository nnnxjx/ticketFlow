package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ticketflow.common.AppException;
import com.ticketflow.common.CatalogAccess;
import com.ticketflow.common.Ids;
import com.ticketflow.common.UserContext;
import com.ticketflow.config.TicketCrypto;
import com.ticketflow.entity.order.OrderEntity;
import com.ticketflow.entity.order.OrderItemEntity;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.ticket.CheckinEntity;
import com.ticketflow.entity.ticket.TicketEntity;
import com.ticketflow.mapper.ticket.TicketMapper;
import com.ticketflow.service.ICheckinService;
import com.ticketflow.service.IOrderItemService;
import com.ticketflow.service.IOrderService;
import com.ticketflow.service.ITicketService;
import com.ticketflow.vo.ticket.CheckinView;
import com.ticketflow.vo.ticket.TicketView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicketServiceImpl extends ServiceImpl<TicketMapper, TicketEntity> implements ITicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);
    @Autowired private IOrderService orders;
    @Autowired private IOrderItemService items;
    @Autowired private ICheckinService checkins;
    @Autowired private CatalogAccess access;
    @Autowired private TicketCrypto crypto;
    @Autowired private Ids ids;
    @Autowired private TransactionTemplate transactions;

    @Override
    public int issuePaid(int limit) {
        if (limit < 1 || limit > 500) throw CatalogAccess.invalid("扫描批量不合法");
        List<OrderEntity> pending = orders.list(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getStatus, "PAID").orderByAsc(OrderEntity::getPaidAt).last("LIMIT " + limit));
        int issued = 0;
        for (OrderEntity order : pending) {
            try {
                if (Boolean.TRUE.equals(transactions.execute(status -> issueOne(order.getId())))) issued++;
            } catch (RuntimeException ex) {
                // 唯一键冲突或临时故障会回滚本单；下次扫描补缺票券。
                log.error("Ticket issuance failed for order {}", order.getId(), ex);
            }
        }
        return issued;
    }

    private boolean issueOne(long orderId) {
        OrderEntity order = orders.getById(orderId);
        if (order == null || !"PAID".equals(order.getStatus())) return false;
        List<OrderItemEntity> orderItems = items.list(new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, orderId).orderByAsc(OrderItemEntity::getId));
        if (orderItems.size() != order.getQuantity()) throw new IllegalStateException("Order item count mismatch");
        for (OrderItemEntity item : orderItems) {
            if (ticketForItem(item.getId()) == null) saveTicket(item.getId());
        }
        // 票券写入和订单转 TICKETED 同事务提交；重复任务不能把订单再次出票。
        if (!orders.update(new LambdaUpdateWrapper<OrderEntity>()
                .eq(OrderEntity::getId, orderId).eq(OrderEntity::getStatus, "PAID")
                .set(OrderEntity::getStatus, "TICKETED"))) {
            throw CatalogAccess.conflict("订单出票状态已变化");
        }
        return true;
    }

    private void saveTicket(long itemId) {
        String token = crypto.newToken();
        TicketEntity ticket = new TicketEntity();
        ticket.setId(ids.next());
        ticket.setOrderItemId(itemId);
        ticket.setTokenHash(crypto.hash(token));
        ticket.setTokenCiphertext(crypto.encrypt(token));
        ticket.setKeyVersion(crypto.activeVersion());
        ticket.setStatus("UNUSED");
        ticket.setIssuedAt(LocalDateTime.now(Clock.systemUTC()));
        if (!save(ticket)) throw new IllegalStateException("Ticket insert failed");
    }

    @Override
    public List<TicketView> mine(long orderId) {
        OrderEntity order = orders.getById(orderId);
        if (order == null || !order.getUserId().equals(UserContext.getUserId())) throw CatalogAccess.notFound();
        if (!"TICKETED".equals(order.getStatus())) throw CatalogAccess.conflict("订单尚未出票");
        List<OrderItemEntity> orderItems = items.list(new LambdaQueryWrapper<OrderItemEntity>()
                .eq(OrderItemEntity::getOrderId, orderId).orderByAsc(OrderItemEntity::getId));
        List<TicketView> result = new ArrayList<>(orderItems.size());
        for (OrderItemEntity item : orderItems) {
            TicketEntity ticket = ticketForItem(item.getId());
            if (ticket == null) throw new IllegalStateException("Ticket missing for issued order");
            // 仅持有人可以取回密文中的令牌；令牌不放在路径或日志中。
            result.add(new TicketView(ticket.getId(), item.getId(), ticket.getStatus(),
                    ticket.getIssuedAt(), crypto.decrypt(ticket.getTokenCiphertext(), ticket.getKeyVersion())));
        }
        return result;
    }

    @Override
    public CheckinView checkin(String token) {
        long operatorId = UserContext.getUserId();
        return transactions.execute(status -> checkinOne(token, operatorId));
    }

    private CheckinView checkinOne(String token, long operatorId) {
        TicketEntity ticket;
        try { ticket = getOne(new LambdaQueryWrapper<TicketEntity>().eq(TicketEntity::getTokenHash, crypto.hash(token))); }
        catch (IllegalArgumentException ex) { throw CatalogAccess.notFound(); }
        if (ticket == null) throw CatalogAccess.notFound();
        OrderItemEntity item = items.getById(ticket.getOrderItemId());
        OrderEntity order = item == null ? null : orders.getById(item.getOrderId());
        if (order == null) throw CatalogAccess.notFound();
        // 先核对主办方归属，再检查窗口；其他主办方不能探知票券状态。
        SessionEntity session = access.sessionOwned(order.getSessionId());
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        if (!"TICKETED".equals(order.getStatus()) || now.isBefore(session.getCheckinStart())
                || now.isAfter(session.getCheckinEnd())) {
            throw new AppException(HttpStatus.CONFLICT, "CHECKIN_CLOSED", "当前不在检票时间内");
        }
        if (!update(new LambdaUpdateWrapper<TicketEntity>()
                .eq(TicketEntity::getId, ticket.getId()).eq(TicketEntity::getStatus, "UNUSED")
                .set(TicketEntity::getStatus, "USED").set(TicketEntity::getUsedAt, now))) {
            throw new AppException(HttpStatus.CONFLICT, "TICKET_NOT_USABLE", "票券已检票或不可使用");
        }
        CheckinEntity record = new CheckinEntity();
        record.setId(ids.next());
        record.setTicketId(ticket.getId());
        record.setOperatorId(operatorId);
        record.setCheckedAt(now);
        if (!checkins.save(record)) throw new IllegalStateException("Check-in record insert failed");
        return new CheckinView(ticket.getId(), operatorId, now);
    }

    private TicketEntity ticketForItem(long itemId) {
        return getOne(new LambdaQueryWrapper<TicketEntity>().eq(TicketEntity::getOrderItemId, itemId));
    }
}
