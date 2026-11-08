package com.ticketflow.job;

import com.ticketflow.service.ITicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TicketIssueJob {
    private static final Logger log = LoggerFactory.getLogger(TicketIssueJob.class);
    @Autowired private ITicketService tickets;

    @Scheduled(fixedDelayString = "${ticketflow.ticket.issue-scan-delay-ms:5000}")
    public void issuePaid() {
        try {
            int issued = tickets.issuePaid(100);
            if (issued > 0) log.info("Issued tickets for {} orders", issued);
        } catch (RuntimeException ex) {
            log.error("Ticket issuance scan failed", ex);
        }
    }
}
