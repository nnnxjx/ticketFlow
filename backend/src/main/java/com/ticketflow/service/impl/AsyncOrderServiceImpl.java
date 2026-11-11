package com.ticketflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.common.*;
import com.ticketflow.config.AsyncOrderAmqpConfig;
import com.ticketflow.dto.order.CreateGeneralOrderRequest;
import com.ticketflow.entity.event.EventEntity;
import com.ticketflow.entity.order.OrderEntity;
import com.ticketflow.entity.session.SessionEntity;
import com.ticketflow.entity.tier.TierEntity;
import com.ticketflow.service.*;
import com.ticketflow.vo.order.AsyncOrderView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class AsyncOrderServiceImpl implements IAsyncOrderService {
    private static final Logger log = LoggerFactory.getLogger(AsyncOrderServiceImpl.class);
    @Autowired private IOrderService orders;
    @Autowired private ITierService tiers;
    @Autowired private ISessionService sessions;
    @Autowired private IEventService events;
    @Autowired private AsyncStockGate stockGate;
    @Autowired private RabbitTemplate rabbit;
    @Autowired private ObjectMapper json;

    /** Redis 先原子预占，随后直接投递 MQ；不再持久化一张预请求表。 */
    @Override
    public AsyncOrderView submit(CreateGeneralOrderRequest input) {
        long userId = UserContext.getUserId();
        String requestId = input.getRequestId().trim();
        OrderEntity existing = findOrder(userId, requestId);
        if (existing != null) return ordered(existing, input.getTierId(), input.getQuantity());
        // 判断是不是一次前端超时重试的请求，requestId 和已有的预占令牌一致
        boolean retry = stockGate.matchesToken(userId, requestId, input.getTierId(), input.getQuantity());
        //兜底，参数不一致但是requestId一样,其实基本不可能出现的
        if (!retry && stockGate.hasToken(userId, requestId)) throw requestConflict();

        TierEntity tier = retry ? tiers.getById(input.getTierId()) : purchasable(input.getTierId());
        if (tier == null) throw CatalogAccess.notFound();
        String body = encode(new GeneralOrderMessage(userId, requestId, tier.getId(), input.getQuantity()));
        long result = stockGate.reserve(userId, requestId, tier, input.getQuantity());
        if (result == -6) throw requestConflict();
        if (result == -7) return rejected(userId, requestId, "REQUEST_REJECTED");
        if (result != 1 && result != 2) {
            String code = switch ((int) result) {
                case -1 -> "SOLD_OUT";
                case -2 -> "LIMIT_EXCEEDED";
                case -5 -> "SALE_WINDOW_CLOSED";
                default -> "ASYNC_RECONCILE";
            };
            // 库存键缺失只拒绝本次请求；不因单次缓存异常封停整个票档。
            stockGate.markRejected(userId, requestId, code);
            return rejected(userId, requestId, code);
        }
        // 重复请求复用同一预占令牌，但可再次发消息，弥补“预占后未成功投递”的窗口。
        if (!publish(body, userId, requestId)) {
            OrderEntity completed = findOrder(userId, requestId);
            if (completed != null) return ordered(completed, tier.getId(), input.getQuantity());
            // 确认超时不能证明消息没到消费者，保留令牌供相同 requestId 重发。
            log.warn("Order publish unconfirmed; retry with the same requestId: {}", requestId);
        }
        return new AsyncOrderView(requestId, "PROCESSING", null, null);
    }

    @Override
    public AsyncOrderView status(String requestId) {
        long userId = UserContext.getUserId();
        Long orderId = stockGate.orderId(userId, requestId);
        if (orderId != null) return new AsyncOrderView(requestId, "ORDERED", orderId, null);
        String failure = stockGate.failureCode(userId, requestId);
        if (failure != null) return new AsyncOrderView(requestId, "REJECTED", null, failure);
        if (stockGate.hasToken(userId, requestId)) return new AsyncOrderView(requestId, "PROCESSING", null, null);
        if (stockGate.wasReleased(userId, requestId)) return rejected(userId, requestId, "REQUEST_REJECTED");
        return new AsyncOrderView(requestId, "UNKNOWN", null, null);
    }

    /** MQ 可能重复投递；MySQL 唯一键与条件扣减仍负责最终幂等及库存约束。 */
    @Override
    public void consume(GeneralOrderMessage message) {
        if (message.getUserId() == null || message.getTierId() == null || message.getQuantity() == null
                || message.getQuantity() < 1 || message.getRequestId() == null) {
            throw new IllegalArgumentException("Invalid general order message");
        }
        long userId = message.getUserId();
        String requestId = message.getRequestId();
        long tierId = message.getTierId();
        int quantity = message.getQuantity();
        OrderEntity existing = findOrder(userId, requestId);
        if (existing != null) {
            //做一下判断同一次请求的参数是否一致
            ordered(existing, tierId, quantity);
            stockGate.cacheOrderId(userId, requestId, existing.getId());
            stockGate.confirm(userId, requestId, tierId);
            return;
        }
        // 请求侧发布失败后可能已释放令牌；这种迟到消息不能再建单。
        if (!stockGate.matchesToken(userId, requestId, tierId, quantity)) return;
        long orderId;
        try {
            orderId = orders.createAsync(userId, requestId, tierId, quantity);
        } catch (AppException ex) {
            if (stockGate.release(userId, requestId, tierId)) stockGate.markRejected(userId, requestId, ex.code());
            return;
        }
        // MySQL 事务已提交；先让轮询看到订单，再清理预占令牌。
        stockGate.cacheOrderId(userId, requestId, orderId);
        stockGate.confirm(userId, requestId, tierId);
        log.debug("Async order created: {}", orderId);
    }

    private boolean publish(String body, long userId, String requestId) {
        try {
            CorrelationData correlation = new CorrelationData(userId + ":" + requestId);
            rabbit.convertAndSend(AsyncOrderAmqpConfig.EXCHANGE, AsyncOrderAmqpConfig.ROUTING_KEY, body, correlation);
            return correlation.getFuture().get(5, TimeUnit.SECONDS).isAck();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while publishing order {}", requestId, ex);
            return false;
        } catch (Exception ex) {
            log.warn("Failed to publish order {}", requestId, ex);
            return false;
        }
    }

    private TierEntity purchasable(long tierId) {
        TierEntity tier = tiers.getById(tierId);
        if (tier == null) throw CatalogAccess.notFound();
        SessionEntity session = sessions.getById(tier.getSessionId());
        EventEntity event = session == null ? null : events.getById(session.getEventId());
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        if (!"ON_SALE".equals(tier.getStatus()) || session == null || !"GENERAL".equals(session.getSaleMode())
                || !"ON_SALE".equals(session.getStatus()) || event == null || !"PUBLISHED".equals(event.getStatus())
                || now.isBefore(session.getSaleStart()) || !now.isBefore(session.getSaleEnd())
                || now.isBefore(tier.getSaleStart()) || !now.isBefore(tier.getSaleEnd())) {
            throw CatalogAccess.conflict("场次或票档当前不可购买");
        }
        if (!stockGate.ready(tierId)) throw CatalogAccess.conflict("Redis 库存不可用，请先对账");
        return tier;
    }

    private OrderEntity findOrder(long userId, String requestId) {
        return orders.getOne(new LambdaQueryWrapper<OrderEntity>()
                .eq(OrderEntity::getUserId, userId).eq(OrderEntity::getRequestId, requestId));
    }

    private AsyncOrderView ordered(OrderEntity order, long tierId, int quantity) {
        if (!"GENERAL".equals(order.getSaleMode()) || !Long.valueOf(tierId).equals(order.getTierId())
                || !Integer.valueOf(quantity).equals(order.getQuantity())) throw requestConflict();
        return new AsyncOrderView(order.getRequestId(), "ORDERED", order.getId(), null);
    }

    private AsyncOrderView rejected(long userId, String requestId, String fallback) {
        String code = stockGate.failureCode(userId, requestId);
        return new AsyncOrderView(requestId, "REJECTED", null, code == null ? fallback : code);
    }

    private String encode(GeneralOrderMessage message) {
        try { return json.writeValueAsString(message); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Order message serialization failed", ex); }
    }

    private AppException requestConflict() {
        return new AppException(HttpStatus.CONFLICT, "REQUEST_ID_CONFLICT", "相同 requestId 的购票参数不同");
    }
}
