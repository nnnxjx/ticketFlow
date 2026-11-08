package com.ticketflow.job;

import com.ticketflow.service.IRefundService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefundSettlementJob {
    private static final Logger log = LoggerFactory.getLogger(RefundSettlementJob.class);
    @Autowired private IRefundService refunds;

    @Scheduled(fixedDelayString = "${ticketflow.refund.settle-scan-delay-ms:5000}")
    public void settle() {
        try {
            int completed = refunds.settlePending(100);
            if (completed > 0) log.info("Settled {} simulated refunds", completed);
        } catch (RuntimeException ex) {
            log.error("Refund settlement scan failed", ex);
        }
    }
}
