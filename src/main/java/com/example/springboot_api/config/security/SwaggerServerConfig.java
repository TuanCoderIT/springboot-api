package com.example.springboot_api.config.security;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerServerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server()
                        .url("https://unshapen-splenetically-cheyenne.ngrok-free.dev"))
                .addServersItem(new Server()
                        .url("http://localhost:8386"));
    }
}
