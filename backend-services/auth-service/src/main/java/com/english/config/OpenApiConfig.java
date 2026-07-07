package com.english.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI englishLearningOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("English Learning API")
                        .description("英语学习平台后端接口文档")
                        .version("1.0.0"))
                .servers(List.of(new Server().url("/").description("Current server")));
    }
}
