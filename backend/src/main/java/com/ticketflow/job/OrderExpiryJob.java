package com.ticketflow.job;

import com.ticketflow.service.IOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(OrderExpiryJob.class);
    @Autowired private IOrderService orders;

    @Scheduled(fixedDelayString = "${ticketflow.order.close-scan-delay-ms:30000}")
    public void closeExpired() {
        try {
            int closed = orders.closeExpired(100);
            if (closed > 0) log.info("Closed {} expired orders", closed);
        } catch (RuntimeException ex) {
            // 保留异常供排查；下一轮扫描仍会重新发现尚未关闭的订单。
            log.error("Expired order scan failed", ex);
        }
    }
}
