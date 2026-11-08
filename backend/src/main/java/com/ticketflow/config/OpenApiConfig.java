package com.ticketflow.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "TicketFlow API", version = "v1", description = "票务平台后端接口。鉴权请求头直接填写原始 JWT。"))
@SecurityScheme(name = "jwtAuth", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.HEADER, paramName = "Authorization", description = "只填写原始 JWT，不加 Bearer 前缀")
public class OpenApiConfig {}
