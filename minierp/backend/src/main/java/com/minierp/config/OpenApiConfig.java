package com.minierp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI miniErpOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Mini-ERP API")
                .description("API REST de inventario y facturación para pymes")
                .version("1.0.0"));
    }
}
