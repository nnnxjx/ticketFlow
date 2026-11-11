package com.ticketflow.common;

import com.ticketflow.entity.tier.TierEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class AsyncStockGate {
    private static final Logger log = LoggerFactory.getLogger(AsyncStockGate.class);
    @Autowired private StringRedisTemplate redis;
    private final DefaultRedisScript<Long> reserve = script("redis/reserve.lua");
    private final DefaultRedisScript<Long> release = script("redis/release.lua");
    private final DefaultRedisScript<Long> confirm = script("redis/confirm.lua");
    private final DefaultRedisScript<Long> credit = script("redis/credit.lua");

    private DefaultRedisScript<Long> script(String path) {
        DefaultRedisScript<Long> result = new DefaultRedisScript<>();
        result.setLocation(new ClassPathResource(path));
        result.setResultType(Long.class);
        return result;
    }

    public void initialize(TierEntity tier) {
        // 草稿不可购买；启用事务持有票档行锁，可覆盖失败尝试留下的缓存。
        if (!"DRAFT".equals(tier.getStatus())) throw CatalogAccess.conflict("只能初始化草稿票档库存");
        redis.opsForValue().set(stockKey(tier.getId()), String.valueOf(tier.getAvailableStock()));
    }

    public void removeDraft(long tierId) { redis.delete(stockKey(tierId)); }

    public long reserve(long userId, String requestId, TierEntity tier, int quantity) {
        Long result = redis.execute(reserve,
                List.of(
                        stockKey(tier.getId()),
                        quotaKey(tier.getId(), userId),
                        tokenKey(userId, requestId),
                        resultKey(userId, requestId),
                        markerKey(userId, requestId)
                ),
                        String.valueOf(quantity),
                        String.valueOf(tier.getPerUserLimit()),
                        String.valueOf(tier.getId()),
                        String.valueOf(tier.getSaleStart().toInstant(ZoneOffset.UTC).toEpochMilli()),
                        String.valueOf(tier.getSaleEnd().toInstant(ZoneOffset.UTC).toEpochMilli())
        );
        if (result == null) throw new IllegalStateException("Redis reservation returned no result");
        return result;
    }

    public boolean matchesToken(long userId, String requestId, long tierId, int quantity) {
        return (tierId + ":" + quantity).equals(redis.opsForValue().get(tokenKey(userId, requestId)));
    }

    public boolean hasToken(long userId, String requestId) {
        return Boolean.TRUE.equals(redis.hasKey(tokenKey(userId, requestId)));
    }

    /** 返回 false 表示订单已确认，不能再回补预占。 */
    public boolean release(long userId, String requestId, long tierId) {
        Long result = redis.execute(release, List.of(stockKey(tierId), quotaKey(tierId, userId),
                tokenKey(userId, requestId), markerKey(userId, requestId)));
        if (Long.valueOf(1).equals(result) || Long.valueOf(2).equals(result)) return true;
        if (Long.valueOf(3).equals(result)) return false;
        // 令牌缺失或 Redis 计数异常需要按请求对账，不能据此封停其他购票请求。
        log.error("Redis reservation release needs reconciliation: tierId={}, userId={}, requestId={}, result={}", tierId, userId, requestId, result);
        throw new AppException(HttpStatus.CONFLICT, "ASYNC_RECONCILE", "预占令牌异常，请对账");
    }

    public void confirm(long userId, String requestId, long tierId) {
        Long result = redis.execute(confirm, List.of(tokenKey(userId, requestId), markerKey(userId, requestId)));
        if (Long.valueOf(1).equals(result) || Long.valueOf(2).equals(result)) return;
        log.error("Redis reservation confirmation needs reconciliation: tierId={}, userId={}, requestId={}, result={}", tierId, userId, requestId, result);
        throw new AppException(HttpStatus.CONFLICT, "ASYNC_RECONCILE", "预占令牌异常，请对账");
    }

    public boolean isAsyncOrder(long userId, String requestId) {
        return hasToken(userId, requestId)
                || "CONFIRMED".equals(redis.opsForValue().get(markerKey(userId, requestId)));
    }

    public boolean wasReleased(long userId, String requestId) {
        return "RELEASED".equals(redis.opsForValue().get(markerKey(userId, requestId)));
    }

    public void markRejected(long userId, String requestId, String code) {
        redis.opsForValue().set(resultKey(userId, requestId), code, Duration.ofDays(1));
    }

    public String failureCode(long userId, String requestId) {
        return redis.opsForValue().get(resultKey(userId, requestId));
    }

    /** 订单提交后短期保留轮询结果；重复消息可以刷新同一订单的缓存。 */
    public void cacheOrderId(long userId, String requestId, long orderId) {
        redis.opsForValue().set(orderResultKey(userId, requestId), String.valueOf(orderId), Duration.ofMinutes(30));
    }

    public Long orderId(long userId, String requestId) {
        String value = redis.opsForValue().get(orderResultKey(userId, requestId));
        return value == null ? null : Long.valueOf(value);
    }

    public boolean ready(long tierId) {
        return Boolean.TRUE.equals(redis.hasKey(stockKey(tierId)));
    }

    // 库存回补
    public void credit(long tierId, long userId, int quantity, String operationId, boolean restock) {
        Long result = redis.execute(credit, List.of(stockKey(tierId), quotaKey(tierId, userId), "tf:async:credit:" + operationId),
                String.valueOf(quantity), restock ? "1" : "0");
        if (result == null || result < 0) {
            log.error("Redis stock credit needs reconciliation: tierId={}, userId={}, operationId={}, result={}", tierId, userId, operationId, result);
            throw new AppException(HttpStatus.CONFLICT, "ASYNC_RECONCILE", "Redis 库存差异，请对账");
        }
    }

    private String stockKey(long tierId) { return "tf:async:tier:" + tierId + ":stock"; }
    private String quotaKey(long tierId, long userId) { return "tf:async:tier:" + tierId + ":user:" + userId; }
    private String tokenKey(long userId, String requestId) { return "tf:async:req:" + userId + ":" + requestId; }
    private String markerKey(long userId, String requestId) { return tokenKey(userId, requestId) + ":done"; }
    private String resultKey(long userId, String requestId) { return tokenKey(userId, requestId) + ":result"; }
    private String orderResultKey(long userId, String requestId) { return tokenKey(userId, requestId) + ":order"; }
}
