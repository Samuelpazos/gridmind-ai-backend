package com.gridmind.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GridMind AI REST API")
                        .version("1.0.0")
                        .description("Sistema Inteligente de Monitoreo y Predicción de Fallos en Redes Eléctricas - Documentación Swagger / OpenAPI 3")
                        .contact(new Contact()
                                .name("GridMind AI Engineering Team")
                                .email("contact@gridmind.ai")));
    }
}
