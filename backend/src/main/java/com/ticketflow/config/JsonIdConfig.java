package com.ticketflow.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonIdConfig {
    /** 雪花 ID 超出 JavaScript 安全整数范围；API 以字符串传递 long，避免浏览器舍入。 */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longAsString() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
        };
    }
}
