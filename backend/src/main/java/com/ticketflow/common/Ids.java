package com.ticketflow.common;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class Ids {
    private static final long EPOCH = 1762473600000L;
    private long lastMillis = -1;
    private long sequence = 0;
    @Value("${ticketflow.ids.worker-id:0}") private int workerId;

    public synchronized long next() {
        // 单进程内用毫秒时间与 12 位序列生成 ID；同毫秒序列用尽时等待下一毫秒。
        long now = System.currentTimeMillis();
        if (now < lastMillis) throw new IllegalStateException("System clock moved backwards");
        if (now == lastMillis) {
            sequence = (sequence + 1) & 4095;
            while (sequence == 0 && now <= lastMillis) now = System.currentTimeMillis();
        } else sequence = 0;
        lastMillis = now;
        if (workerId < 0 || workerId > 1023) throw new IllegalStateException("worker-id must be 0..1023");
        // 10 位实例编号 + 12 位毫秒内序号；部署多个实例时每个实例须配置不同 worker-id。
        return ((now - EPOCH) << 22) | ((long) workerId << 12) | sequence;
    }
}
