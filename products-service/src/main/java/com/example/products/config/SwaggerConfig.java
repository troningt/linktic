package com.example.products.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de Swagger/OpenAPI para documentación de la API
 */
@Configuration
public class SwaggerConfig {

  @Value("${server.port:8081}")
  private String serverPort;

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(new Info()
            .title("Products Service API")
            .description("Microservicio para gestión de productos")
            .version("1.0.0")
            .contact(new Contact()
                .name("Development Team")
                .email("dev@example.com")
                .url("https://example.com"))
            .license(new License()
                .name("Apache 2.0")
                .url("https://www.apache.org/licenses/LICENSE-2.0")))
        .servers(List.of(
            new Server()
                .url("http://localhost:" + serverPort + "/api/v1")
                .description("Local Development Server"),
            new Server()
                .url("http://products-service:8081/api/v1")
                .description("Docker Environment Server")))
        .addSecurityItem(new SecurityRequirement().addList("ApiKeyAuth"))
        .components(new io.swagger.v3.oas.models.Components()
            .addSecuritySchemes("ApiKeyAuth",
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-API-Key")
                    .description("API Key para autenticación entre servicios")));
  }
}
