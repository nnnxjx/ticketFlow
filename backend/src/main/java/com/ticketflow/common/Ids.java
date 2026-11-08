package com.ticketflow.common;

import org.springframework.stereotype.Component;

@Component
public class Ids {
    private static final long EPOCH = 1762473600000L;
    private long lastMillis = -1;
    private long sequence = 0;

    public synchronized long next() {
        // 单进程内用毫秒时间与 12 位序列生成 ID；同毫秒序列用尽时等待下一毫秒。
        long now = System.currentTimeMillis();
        if (now < lastMillis) throw new IllegalStateException("System clock moved backwards");
        if (now == lastMillis) {
            sequence = (sequence + 1) & 4095;
            while (sequence == 0 && now <= lastMillis) now = System.currentTimeMillis();
        } else sequence = 0;
        lastMillis = now;
        return ((now - EPOCH) << 12) | sequence;
    }
}
