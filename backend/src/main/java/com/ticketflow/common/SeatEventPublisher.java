package com.ticketflow.common;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SeatEventPublisher {
    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> clients = new ConcurrentHashMap<>();

    public SseEmitter subscribe(long sessionId) {
        SseEmitter emitter = new SseEmitter(300_000L);
        clients.computeIfAbsent(sessionId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable remove = () -> clients.computeIfPresent(sessionId, (ignored, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());
        // 立即写入注释以建立连接，浏览器的 onopen 会据此拉取最新快照。
        try { emitter.send(SseEmitter.event().comment("connected")); }
        catch (IOException ex) { remove.run(); emitter.completeWithError(ex); }
        return emitter;
    }

    /** 只在数据库事务提交后广播；回滚的锁座不能出现在座位图中。 */
    public void changed(long sessionId, long seatId, long version) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { send(sessionId, seatId, version); }
            });
        } else {
            send(sessionId, seatId, version);
        }
    }

    private void send(long sessionId, long seatId, long version) {
        Map<String, Object> payload = Map.of(
                "sessionId", String.valueOf(sessionId),
                "seatId", String.valueOf(seatId),
                "version", String.valueOf(version));
        for (SseEmitter emitter : clients.getOrDefault(sessionId, new CopyOnWriteArrayList<>())) {
            try { emitter.send(SseEmitter.event().data(payload)); }
            catch (IOException | IllegalStateException ex) {
                clients.computeIfPresent(sessionId, (ignored, list) -> {
                    list.remove(emitter);
                    return list.isEmpty() ? null : list;
                });
            }
        }
    }
}
