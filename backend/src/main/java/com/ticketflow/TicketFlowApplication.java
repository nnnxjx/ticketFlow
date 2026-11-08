package com.ticketflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = SecurityAutoConfiguration.class)
@MapperScan("com.ticketflow.mapper")
@EnableScheduling
public class TicketFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketFlowApplication.class, args);
    }
}

